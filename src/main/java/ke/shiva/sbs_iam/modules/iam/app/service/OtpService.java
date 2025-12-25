package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.OtpRecordEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OtpRecordRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRecordRepository otpRecordRepository;
    private final SessionRepository sessionRepository;
    private final UserContactRepository userContactRepository;
//    private final NotificationService notificationService;
    private final OtpGenerator otpGenerator;
    private final PolicyService policyService;


    public void sendOtp(SessionEntity session, NotificationChannel notificationChannel) {

        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(session.getChannel());

        Optional<OtpRecordEntity> existingOtp = otpRecordRepository.findBySessionId(session.getSessionId());
        OtpRecordEntity otpRecord;

        if (existingOtp.isPresent()) {
            otpRecord = existingOtp.get();
            if ("PENDING".equals(otpRecord.getStatus()) &&
                otpRecord.getExpiryTime().isAfter(OffsetDateTime.now()) &&
                otpRecord.getVerifyAttempts() < mfaPolicy.getMaxVerifyAttempts()) {
                log.info("Reusing existing OTP for session: {}", session.getSessionId());
            } else {
                // Update existing record with new OTP
                generateAndSetOtp(otpRecord, session, notificationChannel, mfaPolicy);
                otpRecordRepository.save(otpRecord);
            }
        } else {
            // Create new record
            otpRecord = new OtpRecordEntity();
            otpRecord.setChannel(session.getChannel());
            otpRecord.setNotificationChannel(notificationChannel);
            otpRecord.setSessionId(session.getSessionId());

            generateAndSetOtp(otpRecord, session, notificationChannel, mfaPolicy);

            otpRecordRepository.save(otpRecord);
        }

        // TODO: Get message from template
        // String message = "Your OTP is: " + otp;
//        notificationService.sendSms(session.getIamUser().getMobileNumber(), message);
    }

    private void generateAndSetOtp(OtpRecordEntity otpRecord, SessionEntity session, NotificationChannel notificationChannel, MfaPolicyEntity mfaPolicy) {
        String otp = otpGenerator.generate(mfaPolicy.getOtpType(), mfaPolicy.getOtpLength());
        String otpHash = HashUtil.bcrypt(otp);
        otpRecord.setOtpHash(otpHash);
        otpRecord.setExpiryTime(OffsetDateTime.now().plusSeconds(mfaPolicy.getOtpExpirySeconds()));
        otpRecord.setVerifyAttempts((short) 0);
        otpRecord.setStatus("PENDING");
        otpRecord.setTo(getContactForNotificationChannel(session, notificationChannel));
        log.info("Generated OTP: {} for session: {}", otp, session.getSessionId());
    }

    private String getContactForNotificationChannel(SessionEntity session, NotificationChannel notificationChannel) {
        ContactType contactType = switch (notificationChannel) {
            case EMAIL -> ContactType.EMAIL;
            case SMS, WHATSAPP -> ContactType.PHONE;
        };
        return userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(session.getIamUser(), contactType)
                .map(UserContact::getContactValue)
                .orElseThrow(() -> new IllegalStateException("No primary contact found for notification channel: " + notificationChannel));
    }

    public boolean verify(String sessionId, String otp) {
        Optional<OtpRecordEntity> otpRecordOpt = otpRecordRepository.findBySessionId(sessionId);

        if (otpRecordOpt.isEmpty()) {
            return false;
        }

        OtpRecordEntity otpRecord = otpRecordOpt.get();

        SessionEntity session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            return false;
        }

        if (otpRecord.getExpiryTime().isBefore(OffsetDateTime.now())) {
            otpRecord.setStatus("EXPIRED");
            otpRecordRepository.save(otpRecord);
            return false;
        }

        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(session.getChannel());

        if (otpRecord.getVerifyAttempts() >= mfaPolicy.getMaxVerifyAttempts()) {
            otpRecord.setStatus("BLOCKED");
            otpRecordRepository.save(otpRecord);
            return false;
        }

        boolean matches = HashUtil.bcryptVerify(otp, otpRecord.getOtpHash());

        if (matches) {
            otpRecord.setStatus("VERIFIED");
            otpRecordRepository.save(otpRecord);
            return true;
        } else {
            otpRecord.setVerifyAttempts((short) (otpRecord.getVerifyAttempts() + 1));
            otpRecordRepository.save(otpRecord);
            return false;
        }
    }
}
