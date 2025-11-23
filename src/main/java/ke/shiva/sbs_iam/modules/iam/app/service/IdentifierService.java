package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.IdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.IdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentifierService {

    private final LoginIdentifierRepository identifierRepo;
    private final PolicyEvaluationService policyService;
    private final LoginFlowService loginFlowService;

    public IdentifierResponse handle(IdentifierRequest req) throws AuthException {

        Channel channel = req.getChannel();

        LoginIdentifierEntity identifier = identifierRepo
                .findByIdentifierAndChannelAndStatus_Active(req.getIdentifier(), channel);
        if (identifier == null) {
            throw new AuthException("Identifier not allowed for this channel");
        }

        IamUserEntity user = identifier.getIamUser();

        if (user.getStatus() != IamStatus.ACTIVE) {
            throw new AuthException("User is not active");
        }

        // evaluate policy requirements
        LoginRequirements requirements = policyService.evaluateRequirements(user, channel);

        // create temp session (flow)
        var session = loginFlowService.start(user, channel, requirements);

        IdentifierResponse resp = new IdentifierResponse();
        resp.setFlowId(UUID.fromString(session.getSessionId()));
        resp.setPasswordRequired(true);
        resp.setMfaRequired(requirements.isMfaRequired());
        resp.setPasswordExpired(requirements.isPasswordExpired());
        resp.setFirstLogin(requirements.isFirstLogin());
        resp.setSecurityQuestionsRequired(requirements.isQuestionsRequired());
        resp.setProfileSelectionRequired(requirements.isProfileSelectionRequired());

        return resp;
    }
}