package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Common MFA service that handles OTP/TOTP operations
 * Can be used by both login flow and forgot password flow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommonMfaService {

    private final OtpService otpService;
    private final TotpVerifier totpVerifier;

    /**
     * Send OTP for a session
     *
     * @param session The session entity
     * @param channel The notification channel (SMS, EMAIL, etc.)
     */
    public String sendOtp(SessionEntity session, NotificationChannel channel) {
        log.debug("Sending OTP for session: {} via channel: {}", session.getSessionId(), channel);
        return otpService.sendOtp(session, channel);
    }

    /**
     * Verify OTP code
     *
     * @param sessionId The session ID
     * @param code The OTP code to verify
     * @return true if valid, false otherwise
     */
    public boolean verifyOtp(String sessionId, String code) {
        log.debug("Verifying OTP for session: {}", sessionId);
        return otpService.verify(sessionId, code);
    }

    /**
     * Verify TOTP code
     *
     * @param user The user entity
     * @param code The TOTP code to verify
     * @return true if valid, false otherwise
     */
    public boolean verifyTotp(IamUserEntity user, String code) {
        log.debug("Verifying TOTP for user ID: {}", user.getId());
        return totpVerifier.verify(user, code);
    }

    /**
     * Verify MFA code (automatically determines if OTP or TOTP)
     *
     * @param session The session entity
     * @param code The code to verify
     * @param isTotpRequired Whether TOTP is required
     * @return true if valid, false otherwise
     */
    public boolean verifyMfaCode(SessionEntity session, String code, boolean isTotpRequired) {
        if (isTotpRequired) {
            return verifyTotp(session.getIamUser(), code);
        } else {
            return verifyOtp(session.getSessionId(), code);
        }
    }

    /**
     * Validate MFA code and throw exception if invalid
     *
     * @param session The session entity
     * @param code The code to verify
     * @param isTotpRequired Whether TOTP is required
     * @throws BaseException if code is invalid
     */
    public void validateMfaCode(SessionEntity session, String code, boolean isTotpRequired) {
        boolean valid = verifyMfaCode(session, code, isTotpRequired);
        if (!valid) {
            log.warn("Invalid MFA code for session: {}", session.getSessionId());
            throw BaseException.badRequest("Invalid code");
        }
    }
}
