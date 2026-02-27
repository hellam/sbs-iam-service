package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginHistoryRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RefreshTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RevokedTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDataRetentionJob {

    private final RevokedTokenRepository revokedTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionEventRepository sessionEventRepository;
    private final SecurityEventRepository securityEventRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    @Value("${shiva.security.retention.enabled:true}")
    private boolean enabled;

    @Value("${shiva.security.retention.revoked-tokens-grace-days:2}")
    private int revokedTokenGraceDays;

    @Value("${shiva.security.retention.refresh-tokens-days:30}")
    private int refreshTokenRetentionDays;

    @Value("${shiva.security.retention.session-events-days:180}")
    private int sessionEventRetentionDays;

    @Value("${shiva.security.retention.security-events-days:365}")
    private int securityEventRetentionDays;

    @Value("${shiva.security.retention.login-history-days:365}")
    private int loginHistoryRetentionDays;

    @Scheduled(cron = "${shiva.security.retention.cleanup-cron:0 30 2 * * *}")
    @Transactional
    public void cleanup() {
        if (!enabled) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        long revokedTokenDeleted = revokedTokenRepository.deleteByExpiryDateBefore(
                now.minusDays(Math.max(revokedTokenGraceDays, 0))
        );
        long refreshTokenDeleted = refreshTokenRepository.deleteByExpiresAtBefore(
                now.minusDays(Math.max(refreshTokenRetentionDays, 0))
        );
        long sessionEventDeleted = sessionEventRepository.deleteByEventAtBefore(
                now.minusDays(Math.max(sessionEventRetentionDays, 0))
        );
        long securityEventDeleted = securityEventRepository.deleteByCreatedAtBefore(
                now.minusDays(Math.max(securityEventRetentionDays, 0))
        );
        long loginHistoryDeleted = loginHistoryRepository.deleteByCreatedAtBefore(
                now.minusDays(Math.max(loginHistoryRetentionDays, 0))
        );

        log.info("Security data retention cleanup completed. revokedTokens={}, refreshTokens={}, sessionEvents={}, securityEvents={}, loginHistory={}",
                revokedTokenDeleted, refreshTokenDeleted, sessionEventDeleted, securityEventDeleted, loginHistoryDeleted);
    }
}

