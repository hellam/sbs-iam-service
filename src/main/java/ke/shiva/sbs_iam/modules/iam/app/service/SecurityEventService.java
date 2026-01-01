package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityEventEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    public void onLoginFailure(IamUserEntity user, String reason, SessionEntity session) {
        SecurityEventEntity event = new SecurityEventEntity();
        event.setIamUser(user);
        event.setEventType("LOGIN_FAILURE");
        event.setSeverity("WARN");
        event.setDescription("Failed login attempt: " + reason);
        event.setRelatedSession(session);
        event.setCreatedAt(OffsetDateTime.now());
        // TODO: Populate ipAddress, deviceId, and location from the session or request
        securityEventRepository.save(event);
    }

    public void onLoginSuccess(IamUserEntity user, String reason, SessionEntity session) {
        SecurityEventEntity event = new SecurityEventEntity();
        event.setIamUser(user);
        event.setEventType("LOGIN_SUCCESS");
        event.setSeverity("INFO");
        event.setDescription("Successful login: " + reason);
        event.setRelatedSession(session);
        event.setCreatedAt(OffsetDateTime.now());
        // TODO: Populate ipAddress, deviceId, and location from the session or request
        securityEventRepository.save(event);
    }
}

