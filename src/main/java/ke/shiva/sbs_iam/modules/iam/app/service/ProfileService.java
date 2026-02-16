package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSummary;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.UserProfileResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final LoginFlowService loginFlowService;
    private final OidcTokenService oidcTokenService;
    private final LoginHistoryService loginHistoryService;
    private final EncryptionUtil encryptionUtil;

    public ProfileSelectionResponse listProfiles(UUID flowId){

        SessionEntity session =
                loginFlowService.requireStage(flowId, LoginStage.MFA_OK);

        LoginRequirements reqs =
                loginFlowService.getRequirements(session);

        reviewRequirements(reqs);

        List<ProfileSummary> profiles =
                loginFlowService.getProfiles(session.getIamUser());

        ProfileSelectionResponse resp = new ProfileSelectionResponse();
        resp.setProfiles(profiles);
        return resp;
    }

    @Transactional
    public UserProfileResponse selectProfile(ProfileSelectRequest req, UUID flowId) {

        SessionEntity session =
                loginFlowService.requireStage(flowId, LoginStage.MFA_OK);

        LoginRequirements reqs =
                loginFlowService.getRequirements(session);

        reviewRequirements(reqs);

        loginFlowService.selectProfile(session, req.getProfileType(), Long.valueOf(encryptionUtil.decrypt(req.getProfileId())));
        loginFlowService.updateStage(session, LoginStage.ACTIVE);

        // Extract identifier from session metadata
        String identifier = loginFlowService.extractIdentifier(session);

        // Log successful login completion after profile selection
        loginHistoryService.logLoginSuccess(session.getIamUser(), identifier, session);

        // Issue token with profile claims
        oidcTokenService.issueTokens(session.getId());

        String orgDisplayName = session.getProfileType()== ProfileType.ORG_USER ?
                session.getIamUser().getParty().getOrganization().getDisplayName() : null;

        return UserProfileResponse.builder()
                .identifier(identifier)
                .profileType(session.getProfileType())
                .displayName(session.getIamUser().getParty().getPerson().getFullName())
                .organization(orgDisplayName)
                .build();
    }

    private static void reviewRequirements(LoginRequirements reqs) {
        if (!reqs.isProfileSelectionRequired()) {
            log.warn("Profile selection not required for this channel");
            throw BaseException.badRequest();
        }

        if (reqs.isQuestionsRequired()) {
            log.warn("Security questions not yet completed");
            throw BaseException.invalidFlow();
        }

        if(reqs.isPasswordChangeRequired()) {
            log.warn("Password change not yet completed");
            throw BaseException.invalidFlow();
        }
    }
}


