package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.ForgotPasswordRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordFlowService {

    private final SessionRepository sessionRepo;
    private final SessionEventRepository eventRepo;

    /**
     * Start forgot password flow
     */
    public SessionEntity start(IamUserEntity user, Channel channel, ForgotPasswordRequirements reqs, String identifier, String deviceId) {
        SessionEntity s = new SessionEntity();
        s.setSessionId(String.valueOf(UUID.randomUUID()));
        s.setIamUser(user);
        s.setDeviceId(HashUtil.sha256(deviceId));
        s.setChannel(channel);
        s.setStatus(LoginStage.FP_IDENTIFIER_OK);
        s.setSessionType(SessionType.FORGOT_PASSWORD_TEMP);
        s.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(15)));

        if (s.getMetadata() == null) s.setMetadata(new HashMap<>());
        s.getMetadata().put("requirements", reqs);
        s.getMetadata().put("identifier", identifier);

        sessionRepo.save(s);
        logEvent(s, "FORGOT_PASSWORD_FLOW_STARTED");

        return s;
    }

    /**
     * Get session and validate it's at the required stage
     */
    public SessionEntity requireStage(UUID flowId, LoginStage stage) {
        SessionEntity s = sessionRepo.findBySessionId(String.valueOf(flowId));

        if (s == null)
            throw BaseException.invalidFlow();

        if (s.getExpiresAt().isBefore(OffsetDateTime.now()))
            throw BaseException.sessionExpired("Page expired. Please refresh and try again.");

        if (s.getSessionType() != SessionType.FORGOT_PASSWORD_TEMP)
            throw BaseException.badRequest("Invalid flow type");

        if (s.getStatus() != stage)
            throw BaseException.invalidStage();

        return s;
    }

    /**
     * Get session and validate it's at least at the minimum stage
     */
    public SessionEntity requireAtLeast(UUID flowId, LoginStage minStage) {
        SessionEntity s = sessionRepo.findBySessionId(String.valueOf(flowId));

        if (s == null)
            throw BaseException.invalidFlow();

        if (s.getSessionType() != SessionType.FORGOT_PASSWORD_TEMP)
            throw BaseException.badRequest("Invalid flow type");

        if (s.getStatus().ordinal() < minStage.ordinal())
            throw BaseException.invalidStage();

        return s;
    }

    /**
     * Update flow stage
     */
    public void updateStage(SessionEntity s, LoginStage stage) {
        s.setStatus(stage);
        sessionRepo.save(s);
        logEvent(s, "STAGE_CHANGED_" + stage.name());
    }

    /**
     * Save session
     */
    public void save(SessionEntity session) {
        sessionRepo.save(session);
    }

    /**
     * Complete forgot password flow and clean up session
     */
    public void complete(SessionEntity session) {
        session.setStatus(LoginStage.FP_PASSWORD_RESET);
        session.setRevokedAt(OffsetDateTime.now());
        session.setRevokedReason("Password reset completed");
        sessionRepo.save(session);
        logEvent(session, "FORGOT_PASSWORD_FLOW_COMPLETED");
    }

    /**
     * Log session event
     */
    private void logEvent(SessionEntity session, String eventType) {
        try {
            SessionEventEntity evt = new SessionEventEntity();
            evt.setSession(session);
            evt.setEventType(eventType);
            evt.setEventAt(OffsetDateTime.now());
            eventRepo.save(evt);
        } catch (Exception e) {
            log.warn("Failed to log session event: {}", e.getMessage());
        }
    }
}
