package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.SecurityQuestionsRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostLoginService {

    private final LoginFlowService loginFlowService;
    private final PasswordManager passwordManager;
    private final SecurityQuestionManager questionManager;

    public void changePassword(PasswordChangeRequest req, UUID flowId) {
        if (!req.getNewPassword().equals(req.getNewPasswordConfirmation())) {
            throw BaseException.badRequest("Password confirmation does not match");
        }
        SessionEntity session = loginFlowService.requireAtLeast(flowId, LoginStage.MFA_OK);

        passwordManager.changePassword(session, req);

        // Update requirement flag
        LoginRequirements reqs = loginFlowService.getRequirements(session);
        reqs.setPasswordExpired(false);
        reqs.setFirstLogin(false);

        session.getMetadata().put("requirements", reqs);
        loginFlowService.save(session);
        loginFlowService.extend(session);
    }

    @Transactional
    public void handleQuestions(SecurityQuestionsRequest req, UUID flowId) {
        SessionEntity session = loginFlowService.requireAtLeast(flowId, LoginStage.MFA_OK);
        IamUserEntity user = session.getIamUser();

        questionManager.save(user, req.getQuestions());

        LoginRequirements reqs = loginFlowService.getRequirements(session);
        reqs.setQuestionsRequired(false);

        session.getMetadata().put("requirements", reqs);
        loginFlowService.save(session);
        loginFlowService.extend(session);
    }
}
