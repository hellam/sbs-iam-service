package ke.shiva.sbs_iam.modules.iam.app.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.app.service.DeviceIdValidator;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

import java.util.UUID;

/**
 * Interceptor that validates device IDs before controller method execution.
 * Acts as middleware similar to Laravel's middleware system, providing
 * declarative device validation via the @RequiresDeviceId annotation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceIdInterceptor implements HandlerInterceptor {

    private final DeviceIdValidator deviceIdValidator;
    private final FlowIdProvider flowIdProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresDeviceId requiresDeviceId = handlerMethod.getMethodAnnotation(RequiresDeviceId.class);

        if (requiresDeviceId == null) {
            // No device ID validation required
            return true;
        }

        // Extract device ID from cookie
        String deviceId = extractDeviceIdFromCookie(request);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            if (requiresDeviceId.required()) {
                log.warn("Device ID required but not found for endpoint: {} {}",
                        request.getMethod(), request.getRequestURI());
                throw BaseException.unauthorized("Device identification required");
            } else {
                // Optional device ID, skip validation
                log.debug("Device ID not found but not required for endpoint: {} {}",
                        request.getMethod(), request.getRequestURI());
                return true;
            }
        }

        // Perform validation based on mode
        DeviceValidationMode mode = requiresDeviceId.mode();
        UUID flowId = null;

        // For SESSION_BOUND mode, we need the flow ID
        if (mode == DeviceValidationMode.SESSION_BOUND) {
            try {
                flowId = flowIdProvider.getFlowId();
            } catch (Exception e) {
                log.error("Failed to extract flow ID for SESSION_BOUND validation: {}", e.getMessage());
                throw BaseException.badRequest("Invalid request");
            }
        }

        // Validate the device ID
        deviceIdValidator.validate(deviceId, mode, flowId);

        log.debug("Device ID validation passed for endpoint: {} {} (mode: {})",
                request.getMethod(), request.getRequestURI(), mode);

        return true;
    }

    /**
     * Extracts the device ID from the request cookie.
     */
    private String extractDeviceIdFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME);
        return cookie != null ? cookie.getValue() : null;
    }
}

