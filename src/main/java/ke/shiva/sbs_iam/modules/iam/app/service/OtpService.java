package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.client.notification.v1.dto.SendNotificationResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.OtpRecordEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.infra.external.NotificationService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OtpRecordRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.MaskingUtil;
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
public class OtpService {

    private final OtpRecordRepository otpRecordRepository;
    private final SessionRepository sessionRepository;
    private final UserContactRepository userContactRepository;
    private final OtpGenerator otpGenerator;
    private final PolicyService policyService;
    private final AccountLockoutService accountLockoutService;
    private final NotificationService notificationService;

    public String sendOtp(SessionEntity session, NotificationChannel notificationChannel) {
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

        String contactValue = "";
        if (existingOtp.isPresent()) {
            otpRecord = existingOtp.get();
            if ("PENDING".equals(otpRecord.getStatus()) &&
                    otpRecord.getExpiryTime().isAfter(OffsetDateTime.now()) &&
                    otpRecord.getVerifyAttempts() < mfaPolicy.getMaxVerifyAttempts()) {
                log.info("Reusing existing OTP for session: {}", session.getSessionId());
//                throw BaseException.reuseOtp("An OTP has already been sent and is still valid. Please check your " +
//                        notificationChannel.name().toLowerCase() + ".");
                return "An OTP has already been sent and is still valid. Please check your " +
                        notificationChannel.name().toLowerCase() + ".";
            } else {
                contactValue = generateAndSetOtp(otpRecord, session, notificationChannel, mfaPolicy);
                otpRecordRepository.save(otpRecord);
            }
        } else {
            otpRecord = new OtpRecordEntity();
            otpRecord.setChannel(session.getChannel());
            otpRecord.setNotificationChannel(notificationChannel);
            otpRecord.setSessionId(session.getSessionId());
            contactValue = generateAndSetOtp(otpRecord, session, notificationChannel, mfaPolicy);
            otpRecordRepository.save(otpRecord);
        }

        return contactValue;
    }

    private String generateAndSetOtp(OtpRecordEntity otpRecord, SessionEntity session,
                                     NotificationChannel notificationChannel, MfaPolicyEntity mfaPolicy) {
        String otp = otpGenerator.generate(mfaPolicy.getOtpType(), mfaPolicy.getOtpLength());
        String otpHash = HashUtil.bcrypt(otp);
        UserContact userContact = getContactForNotificationChannel(session, notificationChannel);
        String contactValue = userContact.getContactValue();
        ContactType contactType = userContact.getContactType();

        otpRecord.setOtpHash(otpHash);
        otpRecord.setExpiryTime(OffsetDateTime.now().plusSeconds(mfaPolicy.getOtpExpirySeconds()));
        otpRecord.setVerifyAttempts((short) 0);
        otpRecord.setStatus("PENDING");
        otpRecord.setTo(contactValue);

        //Integrates with notification service to send the OTP via the specified channel
        SendNotificationResponse notificationResponse = notificationService.sendOtp(notificationChannel, contactValue, otp,mfaPolicy.getOtpExpirySeconds());

        //mask contact value for return
        if (contactType == ContactType.EMAIL)
            contactValue = MaskingUtil.maskEmail(contactValue);
        else if (contactType == ContactType.PHONE)
            contactValue = MaskingUtil.maskPhone(contactValue);

        log.info("Generated OTP to: {} for session: {}", contactValue, session.getSessionId());
        return "SENT".equals(notificationResponse.getStatus()) ? "OTP sent to " + contactValue + " via " + notificationChannel.getDescription() : notificationResponse.getMessage();
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

        //check if OTP is already blocked or verified
        if (otpRecord.getStatus().equals("BLOCKED") || otpRecord.getStatus().equals("VERIFIED")) {
            return false;
        }

        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(session.getChannel());

        // Check if max verification attempts reached
        if (otpRecord.getVerifyAttempts() >= mfaPolicy.getMaxVerifyAttempts()) {
            otpRecord.setStatus("BLOCKED");
            otpRecordRepository.save(otpRecord);


            // Lock the account
            accountLockoutService.lockAccountAttemptFailure(session.getIamUser(), session.getChannel());

            throw BaseException.accountLocked(
                    "Too many failed OTP verification attempts. Please request a new OTP."
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
                accountLockoutService.lockAccountAttemptFailure(session.getIamUser(), session.getChannel());

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

