package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordLoginRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.PasswordStepResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordAuthService {

    private final LoginFlowService flowService;
    private final PasswordVerifier passwordVerifier;
    private final PolicyEvaluationService policyService;
    private final SecurityEventService eventService;

    public PasswordStepResponse handle(PasswordLoginRequest req) {

        SessionEntity session = flowService.requireStage(req.getFlowId(), LoginStage.IDENTIFIER_OK);
        IamUser user = session.getUser();

        boolean ok = passwordVerifier.verify(user, req.getPassword());
        if (!ok) {
            eventService.failure(user, "PASSWORD_INVALID");
            throw new AuthException("Invalid credentials");
        }

        eventService.success(user, "PASSWORD_SUCCESS");

        LoginRequirements reqs = session.getRequirements();

        flowService.updateStage(session, LoginStage.PASSWORD_OK);

        PasswordStepResponse resp = new PasswordStepResponse();
        resp.setFlowId(session.getId());
        resp.setMfaRequired(reqs.isMfaRequired());
        resp.setPasswordChangeRequired(reqs.isPasswordExpired() || reqs.isFirstLogin());
        resp.setSecurityQuestionsRequired(reqs.isQuestionsRequired());
        resp.setProfileSelectionRequired(reqs.isProfileSelectionRequired());

        return resp;
    }
}

