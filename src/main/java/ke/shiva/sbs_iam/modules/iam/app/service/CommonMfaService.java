package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.TransactionMfaAction;
import ke.shiva.sbs_iam.modules.iam.domain.enums.TransactionMfaMode;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final CustomerAuthRepository customerAuthRepository;
    private final EmployeeAuthRepository employeeAuthRepository;
    private final PolicyService policyService;

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
     * @param code      The OTP code to verify
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
    public boolean verifyTotp(IamUserEntity user, String code, Channel channel) {
        log.debug("Verifying TOTP for user ID: {}", user.getId());
        return totpVerifier.verify(user, code, channel);
    }

    public boolean verify(SessionEntity session, String code) {
        return verify(session, code, null);
    }

    public boolean verify(SessionEntity session, String code, TransactionMfaAction action) {
        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(session.getChannel());

        // Legacy path used by non-transactional flows where TOTP is inferred from enrollment.
        if (action == null) {
            boolean isTotpRequired = isTotpEnrolled(session, mfaPolicy);
            if (!verifyMfaCode(session, code, isTotpRequired)) {
                throw BaseException.badRequest("Invalid code");
            }
            return true;
        }

        if (!isTransactionMfaRequired(mfaPolicy, action)) {
            log.debug("Skipping transaction MFA verification for session={} action={}", session.getSessionId(), action);
            return true;
        }

        if (!StringUtils.hasText(code)) {
            throw BaseException.badRequest("MFA code is required.");
        }

        TransactionMfaMode mode = resolveTransactionMfaMode(mfaPolicy);
        boolean valid = switch (mode) {
            case TOTP -> {
                if (mfaPolicy == null || !Boolean.TRUE.equals(mfaPolicy.getAllowTotp()) || !isTotpEnrolled(session, mfaPolicy)) {
                    throw BaseException.badRequest("TOTP is required for this action. Please enable authenticator setup.");
                }
                yield verifyTotp(session.getIamUser(), code, session.getChannel());
            }
            case OTP -> verifyOtp(session.getSessionId(), code);
        };

        if (!valid) {
            throw BaseException.badRequest("Invalid code");
        }
        return true;
    }

    /**
     * Verify MFA code (automatically determines if OTP or TOTP)
     *
     * @param session        The session entity
     * @param code           The code to verify
     * @param isTotpRequired Whether TOTP is required
     * @return true if valid, false otherwise
     */
    public boolean verifyMfaCode(SessionEntity session, String code, boolean isTotpRequired) {
        if (isTotpRequired) {
            return verifyTotp(session.getIamUser(), code, session.getChannel());
        } else {
            return verifyOtp(session.getSessionId(), code);
        }
    }

    /**
     * Validate MFA code and throw exception if invalid
     *
     * @param session        The session entity
     * @param code           The code to verify
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

    private boolean isTransactionMfaRequired(MfaPolicyEntity mfaPolicy, TransactionMfaAction action) {
        if (mfaPolicy == null || action == null) {
            return true;
        }
        return switch (action) {
            case INITIATION -> !Boolean.FALSE.equals(mfaPolicy.getEnforceOnTransactionInitiation());
            case APPROVAL -> !Boolean.FALSE.equals(mfaPolicy.getEnforceOnTransactionApproval());
            case REJECTION -> !Boolean.FALSE.equals(mfaPolicy.getEnforceOnTransactionRejection());
        };
    }

    private TransactionMfaMode resolveTransactionMfaMode(MfaPolicyEntity mfaPolicy) {
        if (mfaPolicy == null || mfaPolicy.getTransactionMfaMode() == null) {
            return TransactionMfaMode.OTP;
        }
        return mfaPolicy.getTransactionMfaMode();
    }

    private boolean isTotpEnrolled(SessionEntity session, MfaPolicyEntity mfaPolicy) {
        return switch (session.getChannel()) {
            case INTERNET_BANKING, MOBILE_BANKING -> {
                CustomerAuthEntity customerProfile = customerAuthRepository.findByIamUser(session.getIamUser());
                yield mfaPolicy != null
                        && Boolean.TRUE.equals(mfaPolicy.getAllowTotp())
                        && customerProfile != null
                        && Boolean.TRUE.equals(customerProfile.getMfaEnabled())
                        && StringUtils.hasText(customerProfile.getMfaSecret());
            }
            case BACKOFFICE -> {
                EmployeeAuthEntity employeeProfile = employeeAuthRepository.findByIamUser(session.getIamUser());
                yield mfaPolicy != null
                        && Boolean.TRUE.equals(mfaPolicy.getAllowTotp())
                        && employeeProfile != null
                        && Boolean.TRUE.equals(employeeProfile.getMfaEnabled())
                        && StringUtils.hasText(employeeProfile.getMfaSecret());
            }
            default -> false;
        };
    }
}
