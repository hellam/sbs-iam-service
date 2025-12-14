package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private final PasswordHistoryRepository passwordHistoryRepo;
    private final MfaPolicyRepository mfaPolicyRepo;
    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final SecurityQuestionRepository securityQuestionRepo;

    public LoginRequirements evaluateRequirements(IamUserEntity user, Channel channel) {

        boolean otpRequired = isOtpRequired(channel);
        boolean totpRequired = isTotpRequired(user, channel);
        boolean passwordExpired = isPasswordExpired(user, channel);
        boolean firstLogin = isFirstLogin(user);
        boolean questionsRequired = areSecurityQuestionsRequired(user, channel);
        boolean profileSelectionRequired = isProfileSelectionRequired(user, channel);

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
        MfaPolicyEntity mfaPolicy = mfaPolicyRepo.findByChannel(channel);
        return mfaPolicy != null;
    }

    private boolean isTotpRequired(IamUserEntity user, Channel channel) {
        MfaPolicyEntity mfaPolicy = mfaPolicyRepo.findByChannel(channel);
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

    private boolean isFirstLogin(IamUserEntity user) {
        // e.g. if user.getLastLoginAt() == null
        return user.getLastLoginAt() == null;
    }

    private boolean areSecurityQuestionsRequired(IamUserEntity user, Channel channel) {
        // TODO: check SecurityQuestionPolicy & whether user already has answers
        return false;
    }

    private boolean isProfileSelectionRequired(IamUserEntity user, Channel channel) {
        return channel == Channel.INTERNET_BANKING;
    }
}
