package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.app.security.DeviceValidationMode;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.DeviceEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityEventEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.DeviceRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.RequestUtil;
import ke.shiva.shivacorestarter.exception.BaseException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for validating device IDs in various authentication contexts.
 * Provides centralized logic for device verification across the authentication flow
 * and updates last‑seen information on success.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceIdValidator {

    private final DeviceRepository deviceRepository;
    private final SessionRepository sessionRepository;
    private final GeoIpService geoIpService;
    private final SecurityEventRepository securityEventRepository;

    /**
     * Validates a device ID based on the specified validation mode and updates
     * the last‑seen metadata if validation succeeds.
     *
     * @param deviceId The raw device ID from the cookie
     * @param mode     The validation mode to apply
     * @param flowId   The flow ID for session‑bound validation (optional for EXISTENCE_ONLY)
     * @throws BaseException if validation fails
     */
    public void validate(String deviceId, DeviceValidationMode mode, UUID flowId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            log.warn("Device ID validation failed: Device ID is missing");
            throw BaseException.unauthorized("Access denied: Invalid request");
        }

        String hashedDeviceId = HashUtil.sha256(deviceId);

        switch (mode) {
            case EXISTENCE_ONLY -> validateExistence(hashedDeviceId);
            case SESSION_BOUND -> validateSessionBound(hashedDeviceId, flowId);
            default -> throw new IllegalArgumentException("Unknown validation mode: " + mode);
        }

        // Update last seen only after validation passes
        updateLastSeen(hashedDeviceId);
    }

    /**
     * Validates that a device ID exists and is active in the database.
     */
    private void validateExistence(String hashedDeviceId) {
        DeviceEntity device = deviceRepository.findByDeviceIdAndActiveTrue(hashedDeviceId)
                .orElseThrow(() -> {
                    log.warn("Device validation failed: Device not found or inactive - deviceId hash: {}",
                            hashedDeviceId.substring(0, 8) + "...");
                    return BaseException.unauthorized("Invalid device");
                });

        log.debug("Device validation successful (EXISTENCE_ONLY): deviceId hash: {}",
                hashedDeviceId.substring(0, 8) + "...");
    }

    /**
     * Validates that a device ID exists and matches the device ID in the active session.
     */
    private void validateSessionBound(String hashedDeviceId, UUID flowId) {
        if (flowId == null) {
            log.error("Device validation failed: Flow ID required for SESSION_BOUND validation");
            throw BaseException.badRequest("Invalid request");
        }

        // First validate existence
        validateExistence(hashedDeviceId);

        // Then verify session binding
        SessionEntity session = sessionRepository.findBySessionId(flowId.toString());

        if (session == null) {
            log.warn("Session not found for flow ID: {}", flowId);
            throw BaseException.unauthorized("Session expired");
        }

        if (!hashedDeviceId.equals(session.getDeviceId())) {
            log.warn("Device mismatch: Session device {} does not match provided device {} for flow ID: {}",
                    session.getDeviceId() != null ? session.getDeviceId().substring(0, 8) + "..." : "null",
                    hashedDeviceId.substring(0, 8) + "...",
                    flowId);
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                log.info("Cannot log security event: no request context available");
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            String ipAddress = RequestUtil.getClientIp(request);

            // Lookup location using GeoIP service
            GeoIpService.GeoLocation location = geoIpService.lookup(ipAddress);

            // Potential security incident: log and throw exception
            SecurityEventEntity securityEvent = new SecurityEventEntity();
            securityEvent.setEventType("DEVICE_MISMATCH");
            securityEvent.setDeviceId(hashedDeviceId);
            securityEvent.setDescription("Device ID mismatch detected during session validation");
            securityEvent.setRelatedSession(session);
            securityEvent.setCreatedAt(OffsetDateTime.now());
            securityEvent.setIamUser(session.getIamUser());
            securityEvent.setSeverity("HIGH");
            securityEvent.setIpAddress(ipAddress);
            securityEvent.setLocationCountry(location !=null ? location.getCountry() : null);
            securityEvent.setLocationCity(location !=null ? location.getCity() : null);
            securityEventRepository.save(securityEvent);

            //TODO: Consider revoking session or tokens associated with this session
            //TODO: Consider blocking device or marking it as high risk

            throw BaseException.unauthorized("Device mismatch");
        }

        log.debug("Device validation successful (SESSION_BOUND): deviceId hash: {}, flowId: {}",
                hashedDeviceId.substring(0, 8) + "...", flowId);
    }

    /**
     * Checks if a device ID exists without throwing exceptions.
     *
     * @param deviceId The raw device ID from the cookie
     * @return true if device exists and is active
     */
    public boolean exists(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return false;
        }
        String hashedDeviceId = HashUtil.sha256(deviceId);
        return deviceRepository.existsByDeviceId(hashedDeviceId);
    }

    /**
     * Updates the last‑seen metadata for a device.  This method extracts the current
     * request context to obtain the client IP and user agent.  If the request
     * context cannot be determined, the update is skipped.
     *
     * @param hashedDeviceId The SHA‑256 hashed device ID
     */
    private void updateLastSeen(String hashedDeviceId) {
        Optional<DeviceEntity> optionalDevice = deviceRepository.findByDeviceIdAndActiveTrue(hashedDeviceId);
        if (optionalDevice.isEmpty()) {
            return;
        }
        DeviceEntity device = optionalDevice.get();
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            // Extract IP and user agent
            String ipAddress = RequestUtil.getClientIp(request);
            String userAgent = request.getHeader(SecurityConstants.Headers.USER_AGENT_HEADER);
            String userAgentHash = HashUtil.sha256(userAgent);

            // Lookup location using GeoIP service
            GeoIpService.GeoLocation location = geoIpService.lookup(ipAddress);

            device.setLastIp(ipAddress);
            device.setLastSeenAt(Instant.now());
            device.setUserAgentHash(userAgentHash);
            device.setLastCountry(location != null ? location.getCountry() : null);
            device.setLastCity(location != null ? location.getCity() : null);
            device.setUpdatedAt(OffsetDateTime.now());

            deviceRepository.save(device);
            log.debug("Last seen updated for device: {}…", hashedDeviceId.substring(0, 8));
        } catch (Exception e) {
            log.warn("Failed to update last seen for device {}: {}",
                    hashedDeviceId.substring(0, 8) + "...", e.getMessage());
        }
    }
}
