package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSummary;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoginFlowService {

    private final SessionRepository sessionRepo;
    private final SessionEventRepository eventRepo;
    private final CustomerProfileRepository customerRepo;
    private final OrganizationUserRepository orgRepo;
    private final SessionRepository sessionRepository;

    // -------- CREATE LOGIN FLOW --------
    public SessionEntity start(IamUserEntity user, Channel channel, LoginRequirements reqs) {

        SessionEntity s = new SessionEntity();
        s.setSessionId(String.valueOf(UUID.randomUUID()));
        s.setIamUser(user);
        s.setChannel(channel);
        s.setStatus(LoginStage.IDENTIFIER_OK);
        s.setSessionType(SessionType.LOGIN_TEMP);
        s.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(15)));

        // store requirements as JSON in existing "metadata" column if you have it
        if (s.getMetadata() == null) s.setMetadata(new HashMap<>());
        s.getMetadata().put("requirements", reqs);

        sessionRepo.save(s);
        logEvent(s, "LOGIN_FLOW_STARTED");

        return s;
    }

    // -------- GET AND VALIDATE STAGE --------
    public SessionEntity requireStage(UUID flowId, LoginStage stage) throws AuthException {
        SessionEntity s = sessionRepo.findBySessionId(String.valueOf(flowId));

        if (s == null)
            throw new AuthException("Invalid flow");

        if (s.getExpiresAt().isBefore(OffsetDateTime.now()))
            throw new AuthException("Session expired");

        if (s.getStatus() != stage)
            throw new AuthException("Invalid stage");

        return s;
    }

    public SessionEntity requireAtLeast(UUID flowId, LoginStage minStage) throws AuthException {
        SessionEntity s = sessionRepo.findBySessionId(String.valueOf(flowId));

        if (s == null)
            throw new AuthException("Invalid flow");

        if (s.getStatus().ordinal() < minStage.ordinal())
            throw new AuthException("Not allowed at this stage");

        return s;
    }

    // -------- UPDATE STAGE --------
    public void updateStage(SessionEntity s, LoginStage stage) {
        s.setStatus(stage);
        sessionRepo.save(s);
        logEvent(s, "STAGE_CHANGED_" + stage.name());
    }

    // -------- LIST PROFILES FOR IB --------
    public List<ProfileSummary> getProfiles(IamUserEntity iamUser) {

        List<ProfileSummary> list = new ArrayList<>();

        customerRepo.findByIamUser(iamUser).ifPresent(cp -> {
            list.add(new ProfileSummary("CUSTOMER", cp.getId(), cp.getFullName()));
        });

        List<OrganizationUserEntity> orgUsers = orgRepo.findAllByIamUser(iamUser);
        for (var ou : orgUsers) {
            list.add(new ProfileSummary("ORG_USER", ou.getId(), ou.getOrgDisplayName()));
        }

        return list;
    }

    // -------- SELECT PROFILE --------
    public void selectProfile(SessionEntity s, ProfileType type, Long profileId) {
        s.setSessionType(SessionType.LOGIN_ACTIVE);
        s.setProfileType(type);
        s.setProfileId(profileId);

        sessionRepo.save(s);
        logEvent(s, "PROFILE_SELECTED_" + type);
    }

    // -------- EVENT LOGGER --------
    private void logEvent(SessionEntity s, String action) {
        SessionEventEntity event = new SessionEventEntity();
        event.setSession(s);
        event.setEventType(action);
        event.setEventAt(OffsetDateTime.now());
        eventRepo.save(event);
    }

    // ----------------------------
    // Fetch requirements from metadata
    // ----------------------------
    public LoginRequirements getRequirements(SessionEntity s) {
        Object obj = s.getMetadata().get("requirements");
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            LoginRequirements reqs = new LoginRequirements();
            reqs.setFirstLogin(Boolean.TRUE.equals(map.get("firstLogin")));
            reqs.setMfaRequired(Boolean.TRUE.equals(map.get("mfaRequired")));
            reqs.setPasswordExpired(Boolean.TRUE.equals(map.get("passwordExpired")));
            reqs.setQuestionsRequired(Boolean.TRUE.equals(map.get("questionsRequired")));
            reqs.setProfileSelectionRequired(Boolean.TRUE.equals(map.get("profileSelectionRequired")));
            return reqs;
        } else {
            return (LoginRequirements) obj;
        }
    }

    public void save(SessionEntity s) {
        sessionRepository.save(s);
    }

}
