package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostLoginService {

    private final LoginFlowService flowService;
    private final PasswordManager passwordManager;
    private final SecurityQuestionManager questionManager;

    public void changePassword(PasswordChangeRequest req) {
        SessionEntity session = flowService.requireAtLeast(req.getFlowId(), LoginStage.PASSWORD_OK);
        passwordManager.changePassword(session.getUser(), req.getNewPassword());
        session.getRequirements().setPasswordChangeRequired(false);
        flowService.save(session);
    }

    public void handleQuestions(SecurityQuestionsRequest req) {
        SessionEntity session = flowService.requireAtLeast(req.getFlowId(), LoginStage.PASSWORD_OK);
        questionManager.saveQuestions(session.getUser(), req.getQuestions());
        session.getRequirements().setQuestionsRequired(false);
        flowService.save(session);
    }
}

