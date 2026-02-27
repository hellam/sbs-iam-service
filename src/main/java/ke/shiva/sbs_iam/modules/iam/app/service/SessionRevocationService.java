package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.RefreshTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.DeviceEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.DeviceRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RefreshTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRevocationService {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceRepository deviceRepository;
    private final SessionEventRepository sessionEventRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${shiva.security.session-revocation.redis-prefix:iam:revoked-session:}")
    private String revocationRedisPrefix;

    @Value("${shiva.security.session-version.redis-prefix:iam:session-version:}")
    private String sessionVersionRedisPrefix;

    @Transactional
    public void revokeSessionAndDevice(SessionEntity session, String reason) {
        if (session == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean sessionUpdated = false;

        if (session.getRevokedAt() == null) {
            session.setRevokedAt(now);
            session.setRevokedReason(reason);
            sessionUpdated = true;
        }

        List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findBySessionAndRevokedAtIsNull(session);
        for (RefreshTokenEntity token : activeTokens) {
            token.setRevokedAt(now);
            token.setRevokedReason(reason);
            token.setIsActive(false);
        }
        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }

        if (sessionUpdated) {
            sessionRepository.save(session);
        }

        // Publish revocation marker so gateway can block requests immediately.
        cacheRevokedSession(session, reason, now);

        if (session.getDeviceId() != null && !session.getDeviceId().isBlank()) {
            deviceRepository.findByDeviceIdAndActiveTrue(session.getDeviceId()).ifPresent(device -> {
                deactivateDevice(device, reason);
            });
        }

        SessionEventEntity event = new SessionEventEntity();
        event.setSession(session);
        event.setEventType("SESSION_REVOKED");
        event.setEventAt(now);
        event.setDeviceId(session.getDeviceId());
        event.setMetadata(java.util.Map.of("reason", reason, "refresh_tokens_revoked", activeTokens.size()));
        sessionEventRepository.save(event);

        log.warn("Session revoked. sessionId={}, reason={}, revokedRefreshTokens={}",
                session.getSessionId(), reason, activeTokens.size());
    }

    @Transactional
    public int revokeAllActiveSessionsForUser(IamUserEntity iamUser, String reason) {
        if (iamUser == null) {
            return 0;
        }

        List<SessionEntity> activeSessions = sessionRepository.findByIamUserAndRevokedAtIsNull(iamUser);
        int count = 0;
        for (SessionEntity session : activeSessions) {
            revokeSessionAndDevice(session, reason);
            count++;
        }

        if (count > 0) {
            log.warn("Revoked all active sessions for userId={} count={} reason={}",
                    iamUser.getId(), count, reason);
        }
        return count;
    }

    private void deactivateDevice(DeviceEntity device, String reason) {
        device.setActive(false);
        device.setRevokedAt(java.time.Instant.now());
        device.setRevokeReason(reason);
        device.setRiskLevel("HIGH");
        Integer currentRisk = device.getRiskScore();
        device.setRiskScore(currentRisk == null ? 100 : Math.max(currentRisk, 100));
        deviceRepository.save(device);
    }

    private void cacheRevokedSession(SessionEntity session, String reason, OffsetDateTime now) {
        if (session.getSessionId() == null || session.getSessionId().isBlank()) {
            return;
        }
        cacheRevokedSessionId(session.getSessionId(), session.getExpiresAt(), reason, now);
    }

    public void cacheSessionVersion(String sessionId, long version, OffsetDateTime expiresAt) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            // Version cache lives slightly longer than session expiry to avoid race conditions during refresh.
            Duration ttl = Duration.ofHours(12);
            OffsetDateTime now = OffsetDateTime.now();
            if (expiresAt != null && expiresAt.isAfter(now)) {
                ttl = Duration.between(now, expiresAt).plusMinutes(30);
            }
            stringRedisTemplate.opsForValue().set(sessionVersionRedisPrefix + sessionId, String.valueOf(version), ttl);
        } catch (Exception e) {
            log.warn("Failed to cache session version in Redis. sessionId={}, version={}, error={}",
                    sessionId, version, e.getMessage());
        }
    }

    private void cacheRevokedSessionId(String sessionId,
                                       OffsetDateTime expiresAt,
                                       String reason,
                                       OffsetDateTime now) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            // Keep revocation key until access tokens should naturally be dead.
            Duration ttl = Duration.ofHours(1);
            if (expiresAt != null && expiresAt.isAfter(now)) {
                ttl = Duration.between(now, expiresAt).plusMinutes(5);
            }
            stringRedisTemplate.opsForValue().set(revocationRedisPrefix + sessionId, reason, ttl);
        } catch (Exception e) {
            log.warn("Failed to cache revoked session in Redis. sessionId={}, reason={}, error={}",
                    sessionId, reason, e.getMessage());
        }
    }
}
