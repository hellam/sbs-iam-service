package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.IdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.IdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentifierService {

    private final LoginIdentifierRepository identifierRepo;
    private final IamUserRepository userRepo;
    private final PolicyEvaluationService policyService;
    private final LoginFlowService flowService;

    public IdentifierResponse handle(IdentifierRequest req) {

        LoginIdentifier identifier = identifierRepo
                .findValidForChannel(req.getIdentifier(), req.getChannel())
                .orElseThrow(() -> new AuthException("Identifier not allowed or inactive"));

        IamUserEntity user = identifier.getUser();
        policyService.validateUserStatus(user);

        // Determine requirements (password, mfa, questions etc)
        LoginRequirements reqs = policyService.evaluateRequirements(user, req.getChannel());

        // Create temp session
        SessionEntity session = flowService.startFlow(user, req.getChannel(), reqs);

        IdentifierResponse resp = new IdentifierResponse();
        resp.setFlowId(session.getId());
        resp.setPasswordRequired(true);
        resp.setMfaRequired(reqs.isMfaRequired());
        resp.setPasswordExpired(reqs.isPasswordExpired());
        resp.setFirstLogin(reqs.isFirstLogin());
        resp.setSecurityQuestionsRequired(reqs.isQuestionsRequired());

        // IB only
        resp.setProfileSelectionRequired(req.getChannel() == Channel.INTERNET_BANKING);

        return resp;
    }
}

