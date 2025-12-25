package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SecurityQuestionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
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

    public LoginRequirements evaluateRequirements(IamUserEntity user, Channel channel) {

        boolean otpRequired = isOtpRequired(channel);
        boolean totpRequired = isTotpRequired(user, channel);
        boolean passwordExpired = isPasswordExpired(user, channel);
        boolean firstLogin = isFirstLogin(user, channel);
        boolean questionsRequired = areSecurityQuestionsRequired(user, channel);
        boolean profileSelectionRequired = isProfileSelectionRequired(channel);

        return new LoginRequirements(
                otpRequired,
                totpRequired,
                passwordExpired,
                firstLogin,
                questionsRequired,
                profileSelectionRequired
        );
    }


    private boolean isOtpRequired(Channel channel) {
        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(channel);
        return mfaPolicy != null;
    }

    private boolean isTotpRequired(IamUserEntity user, Channel channel) {
        MfaPolicyEntity mfaPolicy = policyService.getMfaPolicy(channel);
        if (mfaPolicy != null && mfaPolicy.getAllowTotp()) {
            return switch (channel) {
                case BACKOFFICE -> {
                    EmployeeAuthEntity empAuth = employeeAuthRepo.findByIamUser(user);
                    yield empAuth.getMfaEnabled() != null && empAuth.getMfaEnabled();
                }
                case INTERNET_BANKING, MOBILE_BANKING -> {
                    CustomerAuthEntity custAuth = customerAuthRepo.findByIamUser(user);
                    yield custAuth.getMfaEnabled() != null && custAuth.getMfaEnabled();
                }
                default -> false;
            };
        }
        return false;
    }

    private boolean isPasswordExpired(IamUserEntity user, Channel channel) {
        PasswordPolicyEntity passwordPolicy = passwordPolicyService.resolvePolicy(channel);
        if (passwordPolicy.getExpirationEnabled()) {

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
        return false;
    }

    private boolean isFirstLogin(IamUserEntity user, Channel channel) {
        PasswordPolicyEntity passwordPolicy = passwordPolicyService.resolvePolicy(channel);
        if (passwordPolicy.getRequireFactoryReset()) {
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
        return false;
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

    private boolean isProfileSelectionRequired(Channel channel) {
        return channel == Channel.INTERNET_BANKING;
    }
}
