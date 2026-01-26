package ke.shiva.sbs_iam.modules.iam.app.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.app.service.DeviceIdValidator;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that performs device ID validation before controller execution.
 * Reads device ID from X-Device-ID header (set by API Gateway).
 * <p>
 * The API Gateway handles device cookie management and forwards the device ID
 * to downstream services via the X-Device-ID header.
 * <p>
 * This filter can be registered multiple times with different URL patterns and
 * validation modes to provide flexible device validation.
 */
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class DeviceIdFilter extends OncePerRequestFilter {

    private final DeviceIdValidator deviceIdValidator;
    private final FlowIdProvider flowIdProvider;
    private final DeviceValidationMode validationMode;
    private final boolean required;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Extract device ID from header (set by API Gateway)
        // Gateway handles the device cookie and forwards the device ID via X-Device-ID header
        String deviceId = request.getHeader(SecurityConstants.Headers.DEVICE_ID);

        log.warn("DeviceIdFilter: Extracted device ID from header: {}", deviceId);

        // If the device ID is missing and not required, skip validation
        if ((deviceId == null || deviceId.isBlank()) && !required) {
            filterChain.doFilter(request, response);
            return;
        }

        // Determine flow ID only for session-bound validation
        UUID flowId = null;
        if (validationMode == DeviceValidationMode.SESSION_BOUND) {
            try {
                flowId = flowIdProvider.getFlowId();
            } catch (Exception ignored) {
                // Flow ID missing or invalid; DeviceIdValidator will handle this
            }
        }

        // Delegate validation (updates last seen on success)
        deviceIdValidator.validate(deviceId, validationMode, flowId);
        filterChain.doFilter(request, response);
    }
}
