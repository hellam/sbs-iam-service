package ke.shiva.sbs_iam.modules.iam.app.util;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.app.service.GeoIpService;
import ke.shiva.shivacorestarter.util.RequestUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Utility class to extract request context information like IP address, user agent, device ID, etc.
 * Uses centralized RequestUtil for consistent extraction across the application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestContextExtractor {

    private final GeoIpService geoIpService;

    @Getter
    @Setter
    public static class RequestContext {
        private String ipAddress;
        private String userAgent;
        private String deviceId;
        private String locationCountry;
        private String locationCity;
    }

    /**
     * Extract request context from the current HTTP request
     */
    public RequestContext extractContext() {
        RequestContext context = new RequestContext();

        try {
            HttpServletRequest request = getCurrentRequest().orElse(null);
            if (request != null) {
                // Use centralized RequestUtil for consistent IP extraction
                String ipAddress = RequestUtil.getClientIp(request);
                context.setIpAddress(ipAddress);

                // Use centralized RequestUtil for user agent
                context.setUserAgent(RequestUtil.getUserAgent(request));

                // Use centralized RequestUtil for device ID
                context.setDeviceId(RequestUtil.getDeviceId(request));

                // First check for location headers from CDN/proxy
                String country = RequestUtil.getCountryCode(request);
                String city = RequestUtil.getCity(request);

                if (country != null) {
                    context.setLocationCountry(country);
                    context.setLocationCity(city);
                } else {
                    // Use GeoIP service to lookup location by IP address
                    GeoIpService.GeoLocation geoLocation = geoIpService.lookup(ipAddress);
                    if (geoLocation != null) {
                        context.setLocationCountry(geoLocation.getCountryCode());
                        context.setLocationCity(geoLocation.getCity());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract request context: {}", e.getMessage());
        }

        return context;
    }

    /**
     * Get the current HTTP request if available
     */
    private Optional<HttpServletRequest> getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? Optional.of(attributes.getRequest()) : Optional.empty();
        } catch (Exception e) {
            log.debug("No request context available: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
