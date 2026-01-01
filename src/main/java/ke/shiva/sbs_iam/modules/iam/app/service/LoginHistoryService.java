package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.app.util.RequestContextExtractor;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.LoginHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Service to log login attempts and track login history
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final RequestContextExtractor requestContextExtractor;

    /**
     * Log a successful identifier verification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIdentifierSuccess(IamUserEntity user, String identifier, SessionEntity session) {
        try {
            LoginHistoryEntity history = createBaseHistory(user, identifier, session);
            history.setSuccess(false); // Not yet fully successful - still needs password
            history.setFailureReason("PENDING_PASSWORD_AUTH");
            loginHistoryRepository.save(history);
            log.debug("Logged identifier verification for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to log identifier success: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a failed identifier verification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIdentifierFailure(String identifier, String channel, String reason) {
        try {
            LoginHistoryEntity history = createBaseHistory(null, identifier, null);
            history.setChannel(channel);
            history.setSuccess(false);
            history.setFailureReason(reason);
            loginHistoryRepository.save(history);
            log.debug("Logged failed identifier verification for: {}", identifier);
        } catch (Exception e) {
            log.error("Failed to log identifier failure: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a successful password authentication
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPasswordSuccess(IamUserEntity user, String identifier, SessionEntity session) {
        try {
            LoginHistoryEntity history = createBaseHistory(user, identifier, session);
            history.setSuccess(false); // Not yet fully successful - may need MFA
            history.setFailureReason("PENDING_MFA");
            loginHistoryRepository.save(history);
            log.debug("Logged password authentication success for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to log password success: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a failed password authentication
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPasswordFailure(IamUserEntity user, String identifier, SessionEntity session, String reason) {
        try {
            LoginHistoryEntity history = createBaseHistory(user, identifier, session);
            history.setSuccess(false);
            history.setFailureReason(reason);
            loginHistoryRepository.save(history);
            log.debug("Logged password authentication failure for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to log password failure: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a successful MFA verification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logMfaSuccess(IamUserEntity user, String identifier, SessionEntity session) {
        try {
            LoginHistoryEntity history = createBaseHistory(user, identifier, session);
            history.setSuccess(false); // Not yet fully successful - may need profile selection
            history.setFailureReason("PENDING_FINALIZATION");
            loginHistoryRepository.save(history);
            log.debug("Logged MFA success for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to log MFA success: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a failed MFA verification
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logMfaFailure(IamUserEntity user, String identifier, SessionEntity session, String reason) {
        try {
            LoginHistoryEntity history = createBaseHistory(user, identifier, session);
            history.setSuccess(false);
            history.setFailureReason(reason);
            loginHistoryRepository.save(history);
            log.debug("Logged MFA failure for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to log MFA failure: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a fully successful login (after all steps completed)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginSuccess(IamUserEntity user, String identifier, SessionEntity session) {
        try {
            LoginHistoryEntity history = createBaseHistory(user, identifier, session);
            history.setSuccess(true);
            history.setFailureReason(null);
            loginHistoryRepository.save(history);
            log.info("Logged successful login for user: {} from IP: {}",
                user.getId(), history.getIpAddress());
        } catch (Exception e) {
            log.error("Failed to log login success: {}", e.getMessage(), e);
        }
    }

    /**
     * Log a general login failure
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFailure(IamUserEntity user, String identifier, SessionEntity session, String reason) {
        try {
            LoginHistoryEntity history = createBaseHistory(user, identifier, session);
            history.setSuccess(false);
            history.setFailureReason(reason);
            loginHistoryRepository.save(history);
            log.warn("Logged login failure for user: {} - Reason: {}",
                user != null ? user.getId() : "unknown", reason);
        } catch (Exception e) {
            log.error("Failed to log login failure: {}", e.getMessage(), e);
        }
    }

    /**
     * Create a base login history entity with common fields populated
     */
    private LoginHistoryEntity createBaseHistory(IamUserEntity user, String identifier, SessionEntity session) {
        LoginHistoryEntity history = new LoginHistoryEntity();
        history.setIamUser(user);
        history.setIdentifierUsed(identifier);
        history.setCreatedAt(OffsetDateTime.now());

        // Extract channel from session if available
        if (session != null && session.getChannel() != null) {
            history.setChannel(session.getChannel().name());
        }

        // Extract request context (IP, user agent, device, location)
        RequestContextExtractor.RequestContext context = requestContextExtractor.extractContext();
        if (context != null) {
            history.setIpAddress(context.getIpAddress());
            history.setUserAgent(context.getUserAgent());
            history.setDeviceId(context.getDeviceId());
            history.setLocationCountry(context.getLocationCountry());
            history.setLocationCity(context.getLocationCity());
        }

        return history;
    }
}

