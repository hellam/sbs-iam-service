package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PasswordHistoryRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.MfaPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PolicyEvaluationService {

    private final PasswordHistoryRepository passwordHistoryRepo;
    private final MfaPolicyRepository mfaPolicyRepo;
    private final SecurityQuestionRepository securityQuestionRepo;

    public LoginRequirements evaluateRequirements(IamUserEntity user, Channel channel) {

        boolean passwordExpired = isPasswordExpired(user, channel);
        boolean firstLogin = isFirstLogin(user);
        boolean mfaRequired = isMfaRequired(user, channel);
        boolean questionsRequired = areSecurityQuestionsRequired(user, channel);
        boolean profileSelectionRequired = (channel == Channel.INTERNET_BANKING);

        return new LoginRequirements(
                mfaRequired,
                passwordExpired,
                firstLogin,
                questionsRequired,
                profileSelectionRequired
        );
    }

    private boolean isPasswordExpired(IamUserEntity user, Channel channel) {
        // TODO: implement based on PasswordPolicy + last change timestamp
        return false;
    }

    private boolean isFirstLogin(IamUserEntity user) {
        // e.g. if user.getLastLoginAt() == null
        return user.getLastLoginAt() == null;
    }

    private boolean isMfaRequired(IamUserEntity user, Channel channel) {
        // TODO: read MfaPolicy based on user + channel (org, segment, etc.)
        return true; // default to safe side
    }

    private boolean areSecurityQuestionsRequired(IamUserEntity user, Channel channel) {
        // TODO: check SecurityQuestionPolicy & whether user already has answers
        return false;
    }
}
