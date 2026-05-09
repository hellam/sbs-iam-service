package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeMfaPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficePasswordPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSecurityQuestionPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSessionPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.SessionPolicyResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeMfaPolicyDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficePasswordPolicyResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeSecurityQuestionPolicyDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeSecuritySettingsResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SecurityQuestionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SessionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.MfaPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PasswordPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionPolicyRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BackofficeSecuritySettingsService {

    private static final Set<Integer> ALLOWED_INACTIVITY_TIMEOUT_SECONDS = Set.of(30, 60, 90, 120, 150, 180, 210, 240);
    private static final Set<Integer> ALLOWED_WARNING_COUNTDOWN_SECONDS = Set.of(10, 30, 60, 90, 120);

    private final PasswordPolicyRepository passwordPolicyRepository;
    private final MfaPolicyRepository mfaPolicyRepository;
    private final SecurityQuestionPolicyRepository securityQuestionPolicyRepository;
    private final SessionPolicyRepository sessionPolicyRepository;

    @Transactional(readOnly = true)
    public BackofficeSecuritySettingsResponse getSettings(Channel channel) {
        PasswordPolicyEntity passwordPolicy = requirePasswordPolicy(channel);
        MfaPolicyEntity mfaPolicy = requireMfaPolicy(channel);
        SecurityQuestionPolicyEntity securityQuestionPolicy = requireSecurityQuestionPolicy(channel);
        SessionPolicyEntity sessionPolicy = requireSessionPolicy(channel);

        return BackofficeSecuritySettingsResponse.builder()
                .channel(channel)
                .passwordPolicy(toPasswordPolicyResponse(passwordPolicy))
                .mfaPolicy(toMfaPolicyResponse(mfaPolicy))
                .securityQuestionPolicy(toSecurityQuestionPolicyResponse(securityQuestionPolicy))
                .sessionPolicy(toSessionPolicyResponse(sessionPolicy))
                .build();
    }

    @Transactional(readOnly = true)
    public SessionPolicyResponse getSessionPolicy(Channel channel) {
        return toSessionPolicyResponse(requireSessionPolicy(channel));
    }

    @Transactional
    public BackofficePasswordPolicyResponse updatePasswordPolicy(Channel channel, BackofficePasswordPolicyUpdateRequest request) {
        PasswordPolicyEntity policy = requirePasswordPolicy(channel);

        if (request.getMinLength() != null) {
            policy.setMinLength(request.getMinLength());
        }
        if (request.getMaxLength() != null) {
            policy.setMaxLength(request.getMaxLength());
        }
        if (request.getRequireUppercase() != null) {
            policy.setRequireUppercase(request.getRequireUppercase());
        }
        if (request.getRequireLowercase() != null) {
            policy.setRequireLowercase(request.getRequireLowercase());
        }
        if (request.getRequireNumber() != null) {
            policy.setRequireNumber(request.getRequireNumber());
        }
        if (request.getRequireSymbol() != null) {
            policy.setRequireSymbol(request.getRequireSymbol());
        }
        if (request.getBlockCommonPasswords() != null) {
            policy.setBlockCommonPasswords(request.getBlockCommonPasswords());
        }
        if (request.getPasswordHistoryCount() != null) {
            policy.setPasswordHistoryCount(request.getPasswordHistoryCount());
        }
        if (request.getExpirationEnabled() != null) {
            policy.setExpirationEnabled(request.getExpirationEnabled());
        }
        if (request.getExpirationDays() != null) {
            policy.setExpirationDays(request.getExpirationDays());
        }
        if (request.getMaxFailedAttempts() != null) {
            policy.setMaxFailedAttempts(request.getMaxFailedAttempts());
        }
        if (request.getLockoutMinutes() != null) {
            policy.setLockoutMinutes(request.getLockoutMinutes());
        }
        if (request.getRequireFactoryReset() != null) {
            policy.setRequireFactoryReset(request.getRequireFactoryReset());
        }
        if (request.getHashAlgorithm() != null && !request.getHashAlgorithm().isBlank()) {
            policy.setHashAlgorithm(request.getHashAlgorithm().trim().toLowerCase(Locale.ROOT));
        }
        if (request.getHashCost() != null) {
            policy.setHashCost(request.getHashCost());
        }

        validatePasswordPolicy(policy);
        return toPasswordPolicyResponse(passwordPolicyRepository.save(policy));
    }

    @Transactional
    public BackofficeMfaPolicyDetailsResponse updateMfaPolicy(Channel channel, BackofficeMfaPolicyUpdateRequest request) {
        MfaPolicyEntity policy = requireMfaPolicy(channel);

        if (request.getAllowedNotificationChannels() != null) {
            List<String> channels = request.getAllowedNotificationChannels().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.trim().toUpperCase(Locale.ROOT))
                    .distinct()
                    .toList();
            policy.setAllowedNotificationChannels(channels);
        }
        if (request.getAllowTotp() != null) {
            policy.setAllowTotp(request.getAllowTotp());
        }
        if (request.getMaxVerifyAttempts() != null) {
            policy.setMaxVerifyAttempts(request.getMaxVerifyAttempts());
        }
        if (request.getOtpType() != null) {
            policy.setOtpType(request.getOtpType());
        }
        if (request.getOtpLength() != null) {
            policy.setOtpLength(request.getOtpLength());
        }
        if (request.getTransactionMfaMode() != null) {
            policy.setTransactionMfaMode(request.getTransactionMfaMode());
        }
        if (request.getEnforceOnTransactionInitiation() != null) {
            policy.setEnforceOnTransactionInitiation(request.getEnforceOnTransactionInitiation());
        }
        if (request.getEnforceOnTransactionApproval() != null) {
            policy.setEnforceOnTransactionApproval(request.getEnforceOnTransactionApproval());
        }
        if (request.getEnforceOnTransactionRejection() != null) {
            policy.setEnforceOnTransactionRejection(request.getEnforceOnTransactionRejection());
        }
        if (request.getOtpExpirySeconds() != null) {
            policy.setOtpExpirySeconds(request.getOtpExpirySeconds());
        }
        if (request.getOtpDailyLimit() != null) {
            policy.setOtpDailyLimit(request.getOtpDailyLimit());
        }
        if (request.getEnforceOnNewDevice() != null) {
            policy.setEnforceOnNewDevice(request.getEnforceOnNewDevice());
        }
        if (request.getEnforceOnNewLocation() != null) {
            policy.setEnforceOnNewLocation(request.getEnforceOnNewLocation());
        }

        validateMfaPolicy(policy);
        return toMfaPolicyResponse(mfaPolicyRepository.save(policy));
    }

    @Transactional
    public BackofficeSecurityQuestionPolicyDetailsResponse updateSecurityQuestionPolicy(
            Channel channel,
            BackofficeSecurityQuestionPolicyUpdateRequest request
    ) {
        SecurityQuestionPolicyEntity policy = requireSecurityQuestionPolicy(channel);

        if (request.getEnabled() != null) {
            policy.setEnabled(request.getEnabled());
        }
        if (request.getMinQuestions() != null) {
            policy.setMinQuestions(request.getMinQuestions());
        }
        if (request.getMaxQuestions() != null) {
            policy.setMaxQuestions(request.getMaxQuestions());
        }
        if (request.getMandatory() != null) {
            policy.setMandatory(request.getMandatory());
        }
        if (request.getAskOnForgotPassword() != null) {
            policy.setAskOnForgotPassword(request.getAskOnForgotPassword());
        }
        if (request.getAskOnSensitiveAction() != null) {
            policy.setAskOnSensitiveAction(request.getAskOnSensitiveAction());
        }
        if (request.getIsActive() != null) {
            policy.setIsActive(request.getIsActive());
        }
        if (request.getMaxVerifyAttempts() != null) {
            policy.setMaxVerifyAttempts(request.getMaxVerifyAttempts());
        }

        validateSecurityQuestionPolicy(policy);
        return toSecurityQuestionPolicyResponse(securityQuestionPolicyRepository.save(policy));
    }

    @Transactional
    public SessionPolicyResponse updateSessionPolicy(Channel channel, BackofficeSessionPolicyUpdateRequest request) {
        SessionPolicyEntity policy = requireSessionPolicy(channel);

        if (request.getInactivityTimeoutSeconds() != null) {
            policy.setInactivityTimeoutSeconds(request.getInactivityTimeoutSeconds());
        }
        if (request.getWarningCountdownSeconds() != null) {
            policy.setWarningCountdownSeconds(request.getWarningCountdownSeconds());
        }

        validateSessionPolicy(policy);
        return toSessionPolicyResponse(sessionPolicyRepository.save(policy));
    }

    private PasswordPolicyEntity requirePasswordPolicy(Channel channel) {
        return passwordPolicyRepository.findFirstByChannel(channel)
                .orElseThrow(() -> BaseException.notFound("Password policy not found for channel: " + channel));
    }

    private MfaPolicyEntity requireMfaPolicy(Channel channel) {
        MfaPolicyEntity policy = mfaPolicyRepository.findByChannel(channel);
        if (policy == null) {
            throw BaseException.notFound("MFA policy not found for channel: " + channel);
        }
        return policy;
    }

    private SecurityQuestionPolicyEntity requireSecurityQuestionPolicy(Channel channel) {
        SecurityQuestionPolicyEntity policy = securityQuestionPolicyRepository.findByChannel(channel);
        if (policy == null) {
            throw BaseException.notFound("Security question policy not found for channel: " + channel);
        }
        return policy;
    }

    private SessionPolicyEntity requireSessionPolicy(Channel channel) {
        SessionPolicyEntity policy = sessionPolicyRepository.findByChannel(channel);
        if (policy == null) {
            throw BaseException.notFound("Session policy not found for channel: " + channel);
        }
        return policy;
    }

    private void validatePasswordPolicy(PasswordPolicyEntity policy) {
        Short minLength = policy.getMinLength();
        Short maxLength = policy.getMaxLength();
        if (minLength != null && minLength < 4) {
            throw BaseException.badRequest("minLength must be at least 4.");
        }
        if (maxLength != null && maxLength > 128) {
            throw BaseException.badRequest("maxLength cannot exceed 128.");
        }
        if (minLength != null && maxLength != null && minLength > maxLength) {
            throw BaseException.badRequest("minLength cannot be greater than maxLength.");
        }
        if (policy.getPasswordHistoryCount() != null && policy.getPasswordHistoryCount() < 0) {
            throw BaseException.badRequest("passwordHistoryCount must be zero or positive.");
        }
        if (policy.getExpirationDays() != null && policy.getExpirationDays() <= 0) {
            throw BaseException.badRequest("expirationDays must be greater than zero.");
        }
        if (policy.getMaxFailedAttempts() != null && policy.getMaxFailedAttempts() <= 0) {
            throw BaseException.badRequest("maxFailedAttempts must be greater than zero.");
        }
        if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() <= 0) {
            throw BaseException.badRequest("lockoutMinutes must be greater than zero.");
        }
    }

    private void validateMfaPolicy(MfaPolicyEntity policy) {
        if (policy.getAllowedNotificationChannels() == null || policy.getAllowedNotificationChannels().isEmpty()) {
            throw BaseException.badRequest("allowedNotificationChannels must contain at least one channel.");
        }
        if (policy.getMaxVerifyAttempts() != null && policy.getMaxVerifyAttempts() <= 0) {
            throw BaseException.badRequest("maxVerifyAttempts must be greater than zero.");
        }
        if (policy.getOtpLength() != null && (policy.getOtpLength() < 4 || policy.getOtpLength() > 8)) {
            throw BaseException.badRequest("otpLength must be between 4 and 8.");
        }
        if (policy.getOtpExpirySeconds() != null && policy.getOtpExpirySeconds() <= 0) {
            throw BaseException.badRequest("otpExpirySeconds must be greater than zero.");
        }
        if (policy.getOtpDailyLimit() != null && policy.getOtpDailyLimit() <= 0) {
            throw BaseException.badRequest("otpDailyLimit must be greater than zero.");
        }
    }

    private void validateSecurityQuestionPolicy(SecurityQuestionPolicyEntity policy) {
        if (policy.getMinQuestions() != null && policy.getMinQuestions() < 0) {
            throw BaseException.badRequest("minQuestions must be zero or positive.");
        }
        if (policy.getMaxQuestions() != null && policy.getMaxQuestions() < 0) {
            throw BaseException.badRequest("maxQuestions must be zero or positive.");
        }
        if (policy.getMinQuestions() != null && policy.getMaxQuestions() != null
                && policy.getMinQuestions() > policy.getMaxQuestions()) {
            throw BaseException.badRequest("minQuestions cannot be greater than maxQuestions.");
        }
        if (policy.getMaxVerifyAttempts() != null && policy.getMaxVerifyAttempts() <= 0) {
            throw BaseException.badRequest("maxVerifyAttempts must be greater than zero.");
        }
    }

    private void validateSessionPolicy(SessionPolicyEntity policy) {
        if (policy.getInactivityTimeoutSeconds() == null
                || !ALLOWED_INACTIVITY_TIMEOUT_SECONDS.contains(policy.getInactivityTimeoutSeconds())) {
            throw BaseException.badRequest("inactivityTimeoutSeconds must be one of 30, 60, 90, 120, 150, 180, 210, or 240 seconds.");
        }
        if (policy.getWarningCountdownSeconds() == null
                || !ALLOWED_WARNING_COUNTDOWN_SECONDS.contains(policy.getWarningCountdownSeconds())) {
            throw BaseException.badRequest("warningCountdownSeconds must be one of 10, 30, 60, 90, or 120 seconds.");
        }
        if (policy.getWarningCountdownSeconds() >= policy.getInactivityTimeoutSeconds()) {
            throw BaseException.badRequest("warningCountdownSeconds must be less than inactivityTimeoutSeconds.");
        }
    }

    private BackofficePasswordPolicyResponse toPasswordPolicyResponse(PasswordPolicyEntity policy) {
        return BackofficePasswordPolicyResponse.builder()
                .channel(policy.getChannel())
                .minLength(policy.getMinLength())
                .maxLength(policy.getMaxLength())
                .requireUppercase(policy.getRequireUppercase())
                .requireLowercase(policy.getRequireLowercase())
                .requireNumber(policy.getRequireNumber())
                .requireSymbol(policy.getRequireSymbol())
                .blockCommonPasswords(policy.getBlockCommonPasswords())
                .passwordHistoryCount(policy.getPasswordHistoryCount())
                .expirationEnabled(policy.getExpirationEnabled())
                .expirationDays(policy.getExpirationDays())
                .maxFailedAttempts(policy.getMaxFailedAttempts())
                .lockoutMinutes(policy.getLockoutMinutes())
                .requireFactoryReset(policy.getRequireFactoryReset())
                .hashAlgorithm(policy.getHashAlgorithm())
                .hashCost(policy.getHashCost())
                .build();
    }

    private BackofficeMfaPolicyDetailsResponse toMfaPolicyResponse(MfaPolicyEntity policy) {
        return BackofficeMfaPolicyDetailsResponse.builder()
                .channel(policy.getChannel())
                .availableNotificationChannels(Arrays.stream(NotificationChannel.values()).map(Enum::name).toList())
                .allowedNotificationChannels(policy.getAllowedNotificationChannels())
                .allowTotp(policy.getAllowTotp())
                .maxVerifyAttempts(policy.getMaxVerifyAttempts())
                .otpType(policy.getOtpType())
                .otpLength(policy.getOtpLength())
                .transactionMfaMode(policy.getTransactionMfaMode())
                .enforceOnTransactionInitiation(policy.getEnforceOnTransactionInitiation())
                .enforceOnTransactionApproval(policy.getEnforceOnTransactionApproval())
                .enforceOnTransactionRejection(policy.getEnforceOnTransactionRejection())
                .otpExpirySeconds(policy.getOtpExpirySeconds())
                .otpDailyLimit(policy.getOtpDailyLimit())
                .enforceOnNewDevice(policy.getEnforceOnNewDevice())
                .enforceOnNewLocation(policy.getEnforceOnNewLocation())
                .build();
    }

    private BackofficeSecurityQuestionPolicyDetailsResponse toSecurityQuestionPolicyResponse(SecurityQuestionPolicyEntity policy) {
        return BackofficeSecurityQuestionPolicyDetailsResponse.builder()
                .channel(policy.getChannel())
                .enabled(policy.getEnabled())
                .minQuestions(policy.getMinQuestions())
                .maxQuestions(policy.getMaxQuestions())
                .mandatory(policy.getMandatory())
                .askOnForgotPassword(policy.getAskOnForgotPassword())
                .askOnSensitiveAction(policy.getAskOnSensitiveAction())
                .isActive(policy.getIsActive())
                .maxVerifyAttempts(policy.getMaxVerifyAttempts())
                .build();
    }

    private SessionPolicyResponse toSessionPolicyResponse(SessionPolicyEntity policy) {
        return SessionPolicyResponse.builder()
                .channel(policy.getChannel())
                .inactivityTimeoutSeconds(policy.getInactivityTimeoutSeconds())
                .warningCountdownSeconds(policy.getWarningCountdownSeconds())
                .build();
    }
}
