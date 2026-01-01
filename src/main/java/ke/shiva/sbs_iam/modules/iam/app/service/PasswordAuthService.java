package ke.shiva.sbs_iam.modules.iam.app.service;


import ke.shiva.sbs_iam.modules.iam.api.request.PasswordLoginRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.PasswordStepResponse;
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
public class PasswordAuthService {

    private final LoginFlowService loginFlowService;
    private final PasswordVerifier passwordVerifier;
    private final SecurityEventService securityEventService; // records LOGIN_FAILURE / SUCCESS
    private final LoginHistoryService loginHistoryService;

    @Transactional
    public PasswordStepResponse handle(PasswordLoginRequest req, UUID flowId) {

        // 1. Load session & ensure correct stage (IDENTIFIER_OK)
        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.IDENTIFIER_OK);
        IamUserEntity user = session.getIamUser();

        // Extract identifier from session metadata or use a default value
        String identifier = loginFlowService.extractIdentifier(session);

        // 2. Verify password against correct credentials table
        boolean ok = passwordVerifier.verify(session, req.getPassword());

        if (!ok) {
            securityEventService.onLoginFailure(user, "PASSWORD_INVALID", session);
            loginHistoryService.logPasswordFailure(user, identifier, session, "PASSWORD_INVALID");
            throw BaseException.unauthorized("Invalid credentials");
        }

        securityEventService.onLoginSuccess(user, "PASSWORD_SUCCESS", session);
        loginHistoryService.logPasswordSuccess(user, identifier, session);

        // 3. Read requirements from metadata
        LoginRequirements reqs = loginFlowService.getRequirements(session);

        // 4. Advance stage & extend session
        loginFlowService.updateStage(session, LoginStage.PASSWORD_OK);
        loginFlowService.extend(session);

        PasswordStepResponse resp = new PasswordStepResponse();
        resp.setFlowId(UUID.fromString(session.getSessionId()));
        resp.setOtpRequired(reqs.isOtpRequired());
        resp.setTotpRequired(reqs.isTotpRequired());
        resp.setPasswordChangeRequired(reqs.isPasswordChangeRequired());
        resp.setSecurityQuestionsRequired(reqs.isQuestionsRequired());
        resp.setProfileSelectionRequired(reqs.isProfileSelectionRequired());

        return resp;
    }
}
