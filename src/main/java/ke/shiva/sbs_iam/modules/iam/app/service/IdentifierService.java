package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.IdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.IdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentifierService {

    private final LoginIdentifierRepository identifierRepo;
    private final PolicyEvaluationService policyService;
    private final LoginFlowService loginFlowService;
    private final LoginHistoryService loginHistoryService;

    @Transactional(readOnly = false)
    public IdentifierResponse handle(IdentifierRequest req){

        Channel channel = req.getChannel();

        LoginIdentifierEntity identifier = identifierRepo
                .findByIdentifierAndChannelAndStatus(req.getIdentifier(), channel, IamStatus.ACTIVE)
                .orElseThrow(() -> {
                    // Log failed identifier verification
                    loginHistoryService.logIdentifierFailure(
                        req.getIdentifier(),
                        channel.name(),
                        "IDENTIFIER_NOT_FOUND"
                    );
                    return BaseException.unauthorized("Invalid credentials");
                });

        IamUserEntity user = identifier.getIamUser();

        if (user.getStatus() != IamStatus.ACTIVE) {
            // Log failed identifier verification due to inactive user
            loginHistoryService.logIdentifierFailure(
                req.getIdentifier(),
                channel.name(),
                "USER_INACTIVE"
            );
            throw BaseException.unauthorized("Invalid credentials");
        }

        // evaluate policy requirements
        LoginRequirements requirements = policyService.evaluateRequirements(user, channel);

        // create temp session (flow)
        var session = loginFlowService.start(user, channel, requirements,req.getIdentifier());

        // Log successful identifier verification
        loginHistoryService.logIdentifierSuccess(user, req.getIdentifier(), session);

        IdentifierResponse resp = new IdentifierResponse();
        resp.setFlowId(UUID.fromString(session.getSessionId()));
        resp.setPasswordRequired(true);
        resp.setOtpRequired(requirements.isOtpRequired());
        resp.setTotpRequired(requirements.isTotpRequired());
        resp.setPasswordExpired(requirements.isPasswordExpired());
        resp.setFirstLogin(requirements.isFirstLogin());
        resp.setSecurityQuestionsRequired(requirements.isQuestionsRequired());
        resp.setProfileSelectionRequired(requirements.isProfileSelectionRequired());

        return resp;
    }
}