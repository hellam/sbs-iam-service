package ke.shiva.sbs_iam.modules.iam.app.util;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.app.service.GeoIpService;
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
                String ipAddress = extractIpAddress(request);
                context.setIpAddress(ipAddress);
                context.setUserAgent(extractUserAgent(request));
                context.setDeviceId(extractDeviceId(request));

                // First check for location headers (manual override)
//                String countryHeader = request.getHeader("X-Country-Code");
//                String cityHeader = request.getHeader("X-City");
//
//                if (countryHeader != null && !countryHeader.isEmpty()) {
//                    context.setLocationCountry(countryHeader);
//                    context.setLocationCity(cityHeader);
//                } else {
                    // Use GeoIP service to lookup location by IP address
                    GeoIpService.GeoLocation geoLocation = geoIpService.lookup(ipAddress);
                    if (geoLocation != null) {
                        context.setLocationCountry(geoLocation.getCountryCode());
                        context.setLocationCity(geoLocation.getCity());
                    }
//                }
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

    /**
     * Extract IP address from request, checking for proxy headers
     */
    private String extractIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract user agent from request
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            // Truncate if too long
            userAgent = userAgent.substring(0, 500);
        }
        return userAgent;
    }

    /**
     * Extract device ID from custom header or generate from available info
     */
    private String extractDeviceId(HttpServletRequest request) {
        // First, check for custom device ID header
        String deviceId = request.getHeader("X-Device-ID");
        if (deviceId != null && !deviceId.isEmpty()) {
            return deviceId;
        }

        // Alternatively, check for other device identification headers
        deviceId = request.getHeader("X-Device-Identifier");
        if (deviceId != null && !deviceId.isEmpty()) {
            return deviceId;
        }

        return null;
    }
}

