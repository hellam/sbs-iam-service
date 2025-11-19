package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSummary;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.ChannelEnum;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginFlowService {

    private final SessionRepository sessionRepo;
    private final SessionEventRepository eventRepo;
    private final CustomerProfileRepository customerRepo;
    private final OrganizationUserRepository orgRepo;

    // -------- CREATE LOGIN FLOW --------
    public SessionEntity start(IamUserEntity user, ChannelEnum channel, LoginRequirements reqs) {

        SessionEntity s = new SessionEntity();
        s.setSessionId(String.valueOf(UUID.randomUUID()));
        s.setIamUser(user);
        s.setChannel(channel);
        s.setStatus(LoginStage.IDENTIFIER_OK);
        s.setSessionType(SessionType.LOGIN_TEMP);
        s.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));

        // store LoginRequirements in metadata (JSON column)
        s.setMetadata(Map.of("requirements", reqs));

        sessionRepo.save(s);
        logEvent(s, "LOGIN_FLOW_STARTED");

        return s;
    }

    // -------- GET AND VALIDATE STAGE --------
    public SessionEntity requireStage(UUID flowId, LoginStage stage) {
        SessionEntity s = sessionRepo.findById(flowId)
                .orElseThrow(() -> new AuthException("Flow not found"));

        if (s.getExpiresAt().isBefore(Instant.now()))
            throw new AuthException("Session expired");

        if (s.getStage() != stage)
            throw new AuthException("Invalid stage");

        return s;
    }

    // -------- UPDATE STAGE --------
    public void updateStage(SessionEntity s, LoginStage stage) {
        s.setStage(stage);
        sessionRepo.save(s);
        logEvent(s, "STAGE_CHANGED_" + stage.name());
    }

    // -------- LIST PROFILES FOR IB --------
    public List<ProfileSummary> getProfiles(Long userId) {

        List<ProfileSummary> list = new ArrayList<>();

        customerRepo.findByUserId(userId).ifPresent(cp -> {
            list.add(new ProfileSummary("CUSTOMER", cp.getId(), cp.getFullName()));
        });

        List<OrganizationUserEntity> orgUsers = orgRepo.findByUserId(userId);
        for (var ou : orgUsers) {
            list.add(new ProfileSummary("ORG_USER", ou.getId(), ou.getDisplayName()));
        }

        return list;
    }

    // -------- SELECT PROFILE --------
    public void selectProfile(SessionEntity s, String type, Long profileId) {
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
        event.setAction(action);
        event.setCreatedAt(Instant.now());
        eventRepo.save(event);
    }
}


