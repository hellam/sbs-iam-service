package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.OtpRecordEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OtpRecordRepository;
import ke.shiva.shivacorestarter.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRecordRepository otpRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_VERIFY_ATTEMPTS = 3;


    public void sendOtp(SessionEntity session) {

        String otp = RandomStringUtils.randomNumeric(OTP_LENGTH);
        String otpHash = passwordEncoder.encode(otp);

        OtpRecordEntity otpRecord = new OtpRecordEntity();
        otpRecord.setChannel(session.getChannel());
        otpRecord.setNotificationChannel(session.getIamUser().getMobileNumber());
        otpRecord.setSessionId(session.getSessionId());
        otpRecord.setOtpHash(otpHash);
        otpRecord.setExpiryTime(OffsetDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpRecord.setVerifyAttempts((short) 0);
        otpRecord.setStatus("PENDING");

        otpRecordRepository.save(otpRecord);

        // TODO: Get message from template
        String message = "Your OTP is: " + otp;
        notificationService.sendSms(session.getIamUser().getMobileNumber(), message);
    }

    public boolean verify(String sessionId, String otp) {
        Optional<OtpRecordEntity> otpRecordOpt = otpRecordRepository.findBySessionId(sessionId);

        if (otpRecordOpt.isEmpty()) {
            return false;
        }

        OtpRecordEntity otpRecord = otpRecordOpt.get();

        if (otpRecord.getExpiryTime().isBefore(OffsetDateTime.now())) {
            otpRecord.setStatus("EXPIRED");
            otpRecordRepository.save(otpRecord);
            return false;
        }

        if (otpRecord.getVerifyAttempts() >= MAX_VERIFY_ATTEMPTS) {
            otpRecord.setStatus("BLOCKED");
            otpRecordRepository.save(otpRecord);
            return false;
        }

        boolean matches = passwordEncoder.matches(otp, otpRecord.getOtpHash());

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
