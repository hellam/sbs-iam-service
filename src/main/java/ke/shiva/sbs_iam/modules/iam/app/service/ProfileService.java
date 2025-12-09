package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSummary;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final LoginFlowService loginFlowService;
    private final OidcTokenService oidcTokenService;

    public ProfileSelectionResponse listProfiles(UUID flowId) throws AuthException {

        SessionEntity session =
                loginFlowService.requireStage(flowId, LoginStage.MFA_OK);

        LoginRequirements reqs =
                loginFlowService.getRequirements(session);

        if (!reqs.isProfileSelectionRequired()) {
            throw new AuthException("Profile selection not required for this channel");
        }

        List<ProfileSummary> profiles =
                loginFlowService.getProfiles(session.getIamUser());

        ProfileSelectionResponse resp = new ProfileSelectionResponse();
        resp.setFlowId(flowId);
        resp.setProfiles(profiles);
        return resp;
    }

    public OidcTokenResponse selectProfile(ProfileSelectRequest req) throws AuthException {

        SessionEntity session =
                loginFlowService.requireStage(req.getFlowId(), LoginStage.MFA_OK);

        loginFlowService.selectProfile(session, req.getProfileType(), req.getProfileId());
        loginFlowService.updateStage(session, LoginStage.ACTIVE);

        // Issue token with profile claims
        return oidcTokenService.issueTokens(session);
    }
}


