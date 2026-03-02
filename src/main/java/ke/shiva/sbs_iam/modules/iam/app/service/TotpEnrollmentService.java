package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.response.TotpEnrollmentInitResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.TotpEnrollmentVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TotpEnrollmentService {

    private static final String PENDING_TOTP_SECRET_KEY = "pendingTotpSecret";

    private final LoginFlowService loginFlowService;
    private final PolicyService policyService;
    private final TotpVerifier totpVerifier;
    private final CustomerAuthRepository customerAuthRepository;
    private final EmployeeAuthRepository employeeAuthRepository;
    private final EncryptionUtil encryptionUtil;

    @Value("${shiva.security.totp.issuer:Shiva Banking}")
    private String totpIssuer;

    public TotpEnrollmentInitResponse initiate(UUID flowId) {
        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.TOTP_ENROLL_REQUIRED);
        validateTotpPolicy(session);

        String secret = totpVerifier.generateSecret();
        Map<String, Object> metadata = ensureMetadata(session);
        metadata.put(PENDING_TOTP_SECRET_KEY, encryptionUtil.encrypt(secret));
        loginFlowService.save(session);

        String accountName = loginFlowService.extractIdentifier(session);
        int digits = totpVerifier.resolveCodeDigits(session.getChannel());
        int periodSeconds = totpVerifier.getTimeStepSeconds();

        return TotpEnrollmentInitResponse.builder()
                .secretKey(secret)
                .otpauthUri(totpVerifier.buildOtpAuthUri(secret, totpIssuer, accountName, digits, periodSeconds))
                .issuer(totpIssuer)
                .accountName(accountName)
                .digits(digits)
                .periodSeconds(periodSeconds)
                .build();
    }

    public TotpEnrollmentVerifyResponse verify(UUID flowId, String code) {
        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.TOTP_ENROLL_REQUIRED);
        validateTotpPolicy(session);

        String encryptedPendingSecret = extractPendingSecret(session);
        String pendingSecret = decryptPendingSecret(encryptedPendingSecret);
        if (!totpVerifier.verifySecret(pendingSecret, code, session.getChannel())) {
            throw BaseException.badRequest("Invalid authenticator code.");
        }

        // Persist encrypted secret and enable TOTP once enrollment verification succeeds.
        saveTotpEnrollment(session, pendingSecret);

        Map<String, Object> metadata = ensureMetadata(session);
        metadata.remove(PENDING_TOTP_SECRET_KEY);

        // Force fresh login so next authentication challenge uses TOTP.
        expireFlowForFreshLogin(session);

        return TotpEnrollmentVerifyResponse.builder()
                .reloginRequired(true)
                .build();
    }

    private void saveTotpEnrollment(SessionEntity session, String secret) {
        switch (session.getChannel()) {
            case INTERNET_BANKING, MOBILE_BANKING -> {
                CustomerAuthEntity customerAuth = customerAuthRepository.findByIamUser(session.getIamUser());
                if (customerAuth == null) {
                    throw BaseException.badRequest("Unable to complete TOTP enrollment for this user.");
                }
                customerAuth.setMfaEnabled(true);
                customerAuth.setMfaSecret(encryptionUtil.encrypt(secret));
                customerAuth.setMfaLastVerifiedAt(OffsetDateTime.now());
                customerAuthRepository.save(customerAuth);
            }
            case BACKOFFICE -> {
                EmployeeAuthEntity employeeAuth = employeeAuthRepository.findByIamUser(session.getIamUser());
                if (employeeAuth == null) {
                    throw BaseException.badRequest("Unable to complete TOTP enrollment for this user.");
                }
                employeeAuth.setMfaEnabled(true);
                employeeAuth.setMfaSecret(encryptionUtil.encrypt(secret));
                employeeAuth.setMfaLastVerifiedAt(OffsetDateTime.now());
                employeeAuthRepository.save(employeeAuth);
            }
            default -> throw BaseException.badRequest("TOTP enrollment is not supported for this channel.");
        }
    }

    private void expireFlowForFreshLogin(SessionEntity session) {
        session.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        session.setRevokedAt(OffsetDateTime.now());
        session.setRevokedReason("TOTP_ENROLLMENT_COMPLETED_RELOGIN_REQUIRED");
        loginFlowService.save(session);
    }

    private void validateTotpPolicy(SessionEntity session) {
        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(session.getChannel());
        if (mfaPolicy == null || !Boolean.TRUE.equals(mfaPolicy.getAllowTotp())) {
            throw BaseException.forbidden("TOTP enrollment is currently disabled.");
        }
    }

    private String extractPendingSecret(SessionEntity session) {
        Map<String, Object> metadata = ensureMetadata(session);
        Object value = metadata.get(PENDING_TOTP_SECRET_KEY);
        if (!(value instanceof String encryptedSecret) || !StringUtils.hasText(encryptedSecret)) {
            throw BaseException.badRequest("Initialize TOTP setup before verification.");
        }
        return encryptedSecret;
    }

    private String decryptPendingSecret(String encryptedSecret) {
        try {
            return encryptionUtil.decrypt(encryptedSecret);
        } catch (Exception exception) {
            throw BaseException.badRequest("Unable to read pending TOTP setup. Please start setup again.");
        }
    }

    private Map<String, Object> ensureMetadata(SessionEntity session) {
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        return session.getMetadata();
    }
}
