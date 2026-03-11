package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.SessionPasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSummary;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.SessionPasswordContextResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.UserProfileResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.security.jwt.JwtClaims;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import ke.shiva.shivacorestarter.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private final OrganizationUserRepository organizationUserRepository;
    private final SessionRepository sessionRepository;
    private final SessionRevocationService sessionRevocationService;
    private final LoginAlertService loginAlertService;
    private final PasswordManager passwordManager;

    @Value("${shiva.security.spa.public-key}")
    private String spaPublicKey;

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

    @Transactional(readOnly = true)
    public ProfileSelectionResponse listSessionProfiles(Jwt jwt) {
        SessionEntity session = resolveActiveSession(jwt);
        List<ProfileSummary> profiles = loginFlowService.getProfiles(session.getIamUser());

        ProfileSelectionResponse response = new ProfileSelectionResponse();
        response.setProfiles(profiles);
        return response;
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

        // For internet banking, profile selection is the final login stage.
        loginAlertService.sendNewLoginAlert(session, identifier);

        return buildUserProfileResponse(session, identifier);
    }

    @Transactional
    public UserProfileResponse switchProfile(ProfileSelectRequest req, Jwt jwt) {
        SessionEntity session = resolveActiveSession(jwt);

        Long decryptedProfileId = Long.valueOf(encryptionUtil.decrypt(req.getProfileId()));
        loginFlowService.selectProfile(session, req.getProfileType(), decryptedProfileId);
        loginFlowService.updateStage(session, LoginStage.ACTIVE);

        oidcTokenService.issueTokens(session.getId());

        String identifier = loginFlowService.extractIdentifier(session);
        return buildUserProfileResponse(session, identifier);
    }

    @Transactional
    public void logoutSession(Jwt jwt) {
        SessionEntity session = resolveSession(jwt);
        sessionRevocationService.revokeSessionOnly(session, "USER_LOGOUT");
    }

    @Transactional(readOnly = true)
    public SessionPasswordContextResponse getSessionPasswordContext(Jwt jwt) {
        SessionEntity session = resolveActiveSession(jwt);
        if (session.getChannel() != Channel.INTERNET_BANKING) {
            throw BaseException.forbidden("Access denied");
        }
        return SessionPasswordContextResponse.builder()
                .sessionId(session.getSessionId())
                .publicKey(FileUtil.cleanPublicKey(spaPublicKey))
                .build();
    }

    @Transactional
    public void changeSessionPassword(SessionPasswordChangeRequest req, Jwt jwt) {
        SessionEntity session = resolveActiveSession(jwt);
        if (session.getChannel() != Channel.INTERNET_BANKING) {
            throw BaseException.forbidden("Access denied");
        }

        PasswordChangeRequest internalRequest = new PasswordChangeRequest();
        internalRequest.setOldPassword(req.getOldPassword());
        internalRequest.setNewPassword(req.getNewPassword());
        internalRequest.setNewPasswordConfirmation(req.getNewPasswordConfirmation());

        passwordManager.changePasswordFromEncryptedRequest(session, internalRequest);
        sessionRevocationService.revokeOtherActiveLoginSessionsForUser(
                session.getIamUser(),
                null,
                "PASSWORD_CHANGED"
        );
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

    private void validateActiveSessionForSwitch(SessionEntity session) {
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw BaseException.sessionExpired("Session expired. Please log in again.");
        }

        if (session.getRevokedAt() != null) {
            throw BaseException.badRequest("Session has been revoked.");
        }

        if (session.getStatus() != LoginStage.ACTIVE || session.getSessionType() != SessionType.LOGIN_ACTIVE) {
            throw BaseException.invalidFlow("Only active sessions can switch profile.");
        }
    }

    private SessionEntity resolveActiveSession(Jwt jwt) {
        SessionEntity session = resolveSession(jwt);
        validateActiveSessionForSwitch(session);
        return session;
    }

    private SessionEntity resolveSession(Jwt jwt) {
        if (jwt == null) {
            throw BaseException.badRequest("Authenticated principal is required.");
        }

        String sessionId = jwt.getClaimAsString(JwtClaims.SESSION_ID);
        if (sessionId == null || sessionId.isBlank()) {
            throw BaseException.badRequest("Session claim is missing from token.");
        }

        SessionEntity session = sessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw BaseException.invalidFlow();
        }
        return session;
    }

    private UserProfileResponse buildUserProfileResponse(SessionEntity session, String identifier) {
        OrganizationUserEntity orgUser = null;
        String orgDisplayName = null;
        if (session.getProfileType() == ProfileType.ORG_USER) {
            orgUser = organizationUserRepository.findById(session.getProfileId())
                    .orElseThrow(() -> BaseException.badRequest("Organization user profile not found"));
            orgDisplayName = orgUser.getOrgDisplayName();
        }

        String displayName = session.getIamUser().getParty() != null && session.getIamUser().getParty().getPerson() != null
                ? session.getIamUser().getParty().getPerson().getFullName()
                : identifier;

        boolean multipleProfiles = loginFlowService.getProfiles(session.getIamUser()).size() > 1;

        return UserProfileResponse.builder()
                .identifier(identifier)
                .profileType(session.getProfileType())
                .displayName(displayName)
                .organization(orgDisplayName)
                .isOrganisation(resolveIsOrganisation(session, orgUser))
                .hasMultipleProfiles(multipleProfiles)
                .build();
    }

    private Boolean resolveIsOrganisation(SessionEntity session, OrganizationUserEntity orgUser) {
        if (session == null || session.getChannel() != Channel.INTERNET_BANKING) {
            return Boolean.FALSE;
        }
        if (session.getProfileType() != ProfileType.ORG_USER || orgUser == null) {
            return Boolean.FALSE;
        }
        if (orgUser.getOrganizationParty() == null || orgUser.getOrganizationParty().getOrganization() == null) {
            return Boolean.FALSE;
        }
        // SME profiles are treated as non-organisation for approvals in internet banking.
        return !Boolean.TRUE.equals(orgUser.getOrganizationParty().getOrganization().getSmeMode());
    }
}
