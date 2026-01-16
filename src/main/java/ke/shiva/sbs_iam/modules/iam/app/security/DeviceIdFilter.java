package ke.shiva.sbs_iam.modules.iam.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.app.service.DeviceIdValidator;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that performs device ID validation before controller execution.  It can
 * be registered multiple times with different URL patterns and validation modes
 * to mirror the behaviour of the old interceptor/annotation system.
 */
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
        // Extract device ID from cookie
        Cookie cookie = WebUtils.getCookie(request, SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME);
        String deviceId = cookie != null ? cookie.getValue() : null;

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
