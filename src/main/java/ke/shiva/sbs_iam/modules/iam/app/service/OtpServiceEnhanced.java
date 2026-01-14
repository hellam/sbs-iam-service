package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.OtpRecordEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OtpRecordRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Enhanced OTP Service with account lockout integration.
 * This is an example of how to integrate AccountLockoutService with OTP verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceEnhanced {

    private final OtpRecordRepository otpRecordRepository;
    private final SessionRepository sessionRepository;
    private final UserContactRepository userContactRepository;
    private final OtpGenerator otpGenerator;
    private final PolicyService policyService;
    private final AccountLockoutService accountLockoutService; // Added for lockout integration

    public void sendOtp(SessionEntity session, NotificationChannel notificationChannel) {
        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(session.getChannel());
        UserContact userContact = getContactForNotificationChannel(session, notificationChannel);

        long otpCount = otpRecordRepository.countByToAndCreatedAtAfter(
                userContact.getContactValue(),
                OffsetDateTime.now().minusDays(1)
        );

        if (otpCount >= mfaPolicy.getOtpDailyLimit()) {
            throw BaseException.tooManyRequests("You have reached the daily limit for OTP requests.");
        }

        Optional<OtpRecordEntity> existingOtp = otpRecordRepository.findBySessionId(session.getSessionId());
        OtpRecordEntity otpRecord;

        if (existingOtp.isPresent()) {
            otpRecord = existingOtp.get();
            if ("PENDING".equals(otpRecord.getStatus()) &&
                    otpRecord.getExpiryTime().isAfter(OffsetDateTime.now()) &&
                    otpRecord.getVerifyAttempts() < mfaPolicy.getMaxVerifyAttempts()) {
                log.info("Reusing existing OTP for session: {}", session.getSessionId());
            } else {
                generateAndSetOtp(otpRecord, session, notificationChannel, mfaPolicy);
                otpRecordRepository.save(otpRecord);
            }
        } else {
            otpRecord = new OtpRecordEntity();
            otpRecord.setChannel(session.getChannel());
            otpRecord.setNotificationChannel(notificationChannel);
            otpRecord.setSessionId(session.getSessionId());
            generateAndSetOtp(otpRecord, session, notificationChannel, mfaPolicy);
            otpRecordRepository.save(otpRecord);
        }
    }

    private void generateAndSetOtp(OtpRecordEntity otpRecord, SessionEntity session,
                                   NotificationChannel notificationChannel, MfaPolicyEntity mfaPolicy) {
        String otp = otpGenerator.generate(mfaPolicy.getOtpType(), mfaPolicy.getOtpLength());
        String otpHash = HashUtil.bcrypt(otp);
        otpRecord.setOtpHash(otpHash);
        otpRecord.setExpiryTime(OffsetDateTime.now().plusSeconds(mfaPolicy.getOtpExpirySeconds()));
        otpRecord.setVerifyAttempts((short) 0);
        otpRecord.setStatus("PENDING");
        otpRecord.setTo(getContactForNotificationChannel(session, notificationChannel).getContactValue());
        log.info("Generated OTP: {} for session: {}", otp, session.getSessionId());
    }

    public UserContact getContactForNotificationChannel(SessionEntity session, NotificationChannel notificationChannel) {
        ContactType contactType = switch (notificationChannel) {
            case EMAIL -> ContactType.EMAIL;
            case SMS, WHATSAPP -> ContactType.PHONE;
        };
        return userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(session.getIamUser(), contactType)
                .orElseThrow(() -> new IllegalStateException("No primary contact found for notification channel: " + notificationChannel));
    }

    /**
     * Enhanced verify method with account lockout integration.
     * Locks the account if too many failed OTP verification attempts occur.
     */
    public boolean verify(String sessionId, String otp) {
        SessionEntity session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            return false;
        }

        Optional<OtpRecordEntity> otpRecordOpt = otpRecordRepository.findBySessionId(sessionId);
        if (otpRecordOpt.isEmpty()) {
            return false;
        }

        OtpRecordEntity otpRecord = otpRecordOpt.get();

        // Check if OTP has expired
        if (otpRecord.getExpiryTime().isBefore(OffsetDateTime.now())) {
            otpRecord.setStatus("EXPIRED");
            otpRecordRepository.save(otpRecord);
            return false;
        }

        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(session.getChannel());

        // Check if max verification attempts reached
        if (otpRecord.getVerifyAttempts() >= mfaPolicy.getMaxVerifyAttempts()) {
            otpRecord.setStatus("BLOCKED");
            otpRecordRepository.save(otpRecord);

            // Lock the user account due to excessive failed OTP attempts
            accountLockoutService.lockAccountForOtpFailure(session.getIamUser(), session.getChannel());

            throw BaseException.accountLocked(
                "Too many failed OTP verification attempts. Your account has been locked. " +
                "Please contact support."
            );
        }

        boolean matches = HashUtil.bcryptVerify(otp, otpRecord.getOtpHash());

        if (matches) {
            otpRecord.setStatus("VERIFIED");
            otpRecordRepository.save(otpRecord);
            return true;
        } else {
            // Increment failed attempts
            short newAttempts = (short) (otpRecord.getVerifyAttempts() + 1);
            otpRecord.setVerifyAttempts(newAttempts);

            // Check if this was the last allowed attempt
            if (newAttempts >= mfaPolicy.getMaxVerifyAttempts()) {
                otpRecord.setStatus("BLOCKED");
                otpRecordRepository.save(otpRecord);

                // Lock the account
                accountLockoutService.lockAccountForOtpFailure(session.getIamUser(), session.getChannel());

                throw BaseException.accountLocked(
                    "Too many failed OTP verification attempts. Your account has been locked. " +
                    "Please contact support."
                );
            }

            otpRecordRepository.save(otpRecord);

            // Return remaining attempts info in log
            log.warn("OTP verification failed for session {}. Attempts: {}/{}",
                    sessionId, newAttempts, mfaPolicy.getMaxVerifyAttempts());

            return false;
        }
    }
}

