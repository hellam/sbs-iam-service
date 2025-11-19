package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSummary;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final LoginFlowService flowService;
    private final OidcTokenService tokenService;

    public ProfileSelectionResponse listProfiles(UUID flowId) {
        Session session = flowService.requireStage(flowId, LoginStage.MFA_OK);

        List<ProfileSummary> profiles =
                flowService.fetchProfiles(session.getUser());

        ProfileSelectionResponse resp = new ProfileSelectionResponse();
        resp.setFlowId(flowId);
        resp.setProfiles(profiles);
        return resp;
    }

    public OidcTokenResponse selectProfile(ProfileSelectRequest req) {
        Session session = flowService.requireStage(req.getFlowId(), LoginStage.MFA_OK);

        flowService.selectProfile(session, req.getProfileType(), req.getProfileId());
        flowService.updateStage(session, LoginStage.COMPLETE);

        return tokenService.issueTokensFor(session);
    }
}

