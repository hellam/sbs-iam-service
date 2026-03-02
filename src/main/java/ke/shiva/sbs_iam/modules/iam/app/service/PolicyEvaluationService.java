package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SecurityQuestionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.ForgotPasswordRequirements;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final PasswordPolicyService passwordPolicyService;
    private final PolicyService policyService;

    public LoginRequirements evaluateLoginRequirements(IamUserEntity user, Channel channel) {
        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(channel);
        boolean totpRequired = isTotpRequired(user, channel, mfaPolicy);
        boolean otpRequired = isOtpRequired(mfaPolicy, totpRequired);
        short otpLength = resolveOtpLength(mfaPolicy);
        boolean passwordExpired = isPasswordExpired(user, channel);
        boolean firstLogin = isFirstLogin(user, channel);
        boolean questionsRequired = areSecurityQuestionsRequired(user, channel);
        boolean profileSelectionRequired = isProfileSelectionRequired(channel);

        return new LoginRequirements(
                otpRequired,
                totpRequired,
                otpLength,
                passwordExpired,
                firstLogin,
                questionsRequired,
                profileSelectionRequired
        );
    }

    public ForgotPasswordRequirements evaluateForgotPasswordRequirements(IamUserEntity user, Channel channel) {
        boolean securityQuestionsRequired = areSecurityQuestionsRequiredForForgotPassword(user, channel);
        int securityQuestionsCount = getSecurityQuestionsCount(channel);
        boolean mfaRequired = policyService.getMfaPolicy(channel) != null;

        return ForgotPasswordRequirements.builder()
                .securityQuestionsRequired(securityQuestionsRequired)
                .securityQuestionsCount(securityQuestionsCount)
                .mfaRequired(mfaRequired)
                .build();
    }

    /**
     * TOTP-enrolled users should be challenged with TOTP only.
     */
    private boolean isOtpRequired(MfaPolicyEntity mfaPolicy, boolean totpRequired) {
        return mfaPolicy != null && !totpRequired;
    }

    private boolean isTotpRequired(IamUserEntity user, Channel channel, MfaPolicyEntity mfaPolicy) {
        if (mfaPolicy != null && Boolean.TRUE.equals(mfaPolicy.getAllowTotp())) {
            return switch (channel) {
                case BACKOFFICE -> {
                    EmployeeAuthEntity empAuth = employeeAuthRepo.findByIamUser(user);
                    yield empAuth != null
                            && Boolean.TRUE.equals(empAuth.getMfaEnabled())
                            && empAuth.getMfaSecret() != null
                            && !empAuth.getMfaSecret().isBlank();
                }
                case INTERNET_BANKING, MOBILE_BANKING -> {
                    CustomerAuthEntity custAuth = customerAuthRepo.findByIamUser(user);
                    yield custAuth != null
                            && Boolean.TRUE.equals(custAuth.getMfaEnabled())
                            && custAuth.getMfaSecret() != null
                            && !custAuth.getMfaSecret().isBlank();
                }
                default -> false;
            };
        }
        return false;
    }

    private short resolveOtpLength(MfaPolicyEntity mfaPolicy) {
        if (mfaPolicy == null || mfaPolicy.getOtpLength() == null) {
            return 6;
        }
        short configured = mfaPolicy.getOtpLength();
        if (configured < 4) {
            return 4;
        }
        return (short) Math.min(configured, 8);
    }

    private boolean isPasswordExpired(IamUserEntity user, Channel channel) {
        PasswordPolicyEntity passwordPolicy = passwordPolicyService.resolvePolicy(channel);
        if (passwordPolicy == null || !Boolean.TRUE.equals(passwordPolicy.getExpirationEnabled())) {
            return false;
        }

        return switch (channel) {
            case BACKOFFICE -> {
                EmployeeAuthEntity empAuth = employeeAuthRepo.findByIamUser(user);
                OffsetDateTime expiry = empAuth.getStaffPasswordExpiry();
                yield expiry != null && expiry.isBefore(OffsetDateTime.now());
            }
            case INTERNET_BANKING, MOBILE_BANKING -> {
                CustomerAuthEntity custAuth = customerAuthRepo.findByIamUser(user);
                OffsetDateTime expiry = custAuth.getInternetPasswordExpiry();
                yield expiry != null && expiry.isBefore(OffsetDateTime.now());
            }
            default -> false;
        };
    }

    private boolean isFirstLogin(IamUserEntity user, Channel channel) {
        PasswordPolicyEntity passwordPolicy = passwordPolicyService.resolvePolicy(channel);
        if (passwordPolicy == null || !Boolean.TRUE.equals(passwordPolicy.getRequireFactoryReset())) {
            return false;
        }

        // 3. Update correct credentials table
        switch (channel) {

            case INTERNET_BANKING -> {
                CustomerAuthEntity auth = customerAuthRepo.findByIamUserId(user.getId())
                        .orElseThrow(() -> BaseException.channelNotAllowed("CustomerAuth missing"));
                return auth.getInternetFirstTimeLogin();
            }

            case MOBILE_BANKING -> {
                CustomerAuthEntity auth = customerAuthRepo.findByIamUserId(user.getId())
                        .orElseThrow(() -> BaseException.channelNotAllowed("CustomerAuth missing"));
                return auth.getMobileFirstTimeLogin();
            }

            case BACKOFFICE -> {
                EmployeeAuthEntity auth = employeeAuthRepo.findByIamUserId(user.getId())
                        .orElseThrow(() -> BaseException.channelNotAllowed("EmployeeAuth missing"));
                return auth.getFirstTimeLogin();
            }

            default -> throw BaseException.channelNotAllowed("Unsupported user category");
        }
    }

    private boolean areSecurityQuestionsRequired(IamUserEntity user, Channel channel) {
        SecurityQuestionPolicyEntity securityQuestionPolicy = policyService.getSecurityQuestionPolicy(channel);
        if (securityQuestionPolicy != null && securityQuestionPolicy.getEnabled()) {
            // Check if user has set up security questions
            // For simplicity, let's assume a method exists to check this
            boolean hasSetQuestions = user.getIamUserSecurityQuestions() != null && !user.getIamUserSecurityQuestions().isEmpty();
            return !hasSetQuestions;
        }
        return false;
    }

    private boolean areSecurityQuestionsRequiredForForgotPassword(IamUserEntity user, Channel channel) {
        SecurityQuestionPolicyEntity securityQuestionPolicy = policyService.getSecurityQuestionPolicy(channel);
        if (securityQuestionPolicy != null && securityQuestionPolicy.getEnabled()
                && securityQuestionPolicy.getAskOnForgotPassword() != null
                && securityQuestionPolicy.getAskOnForgotPassword()) {
            // Check if user has set up security questions
            boolean hasSetQuestions = user.getIamUserSecurityQuestions() != null
                    && !user.getIamUserSecurityQuestions().isEmpty();
            return hasSetQuestions;
        }
        return false;
    }

    private int getSecurityQuestionsCount(Channel channel) {
        SecurityQuestionPolicyEntity securityQuestionPolicy = policyService.getSecurityQuestionPolicy(channel);
        if (securityQuestionPolicy != null && securityQuestionPolicy.getMinQuestions() != null) {
            return securityQuestionPolicy.getMinQuestions();
        }
        return 0;
    }

    private boolean isProfileSelectionRequired(Channel channel) {
        return channel == Channel.INTERNET_BANKING;
    }
}
