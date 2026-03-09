package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.config.AllowedDomainsProperties;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class DomainGuard {

    private final AllowedDomainsProperties properties;

    public DomainGuard(AllowedDomainsProperties properties) {
        this.properties = properties;
    }

    public void validate(Channel channel, HttpServletRequest request) {
        Map<Channel, List<String>> map = properties.getMap();
        List<String> allowed = map.get(channel);
        if (allowed == null || allowed.isEmpty()) {
            return; // if not configured, don't restrict (or flip to strict)
        }

        String requestHost = resolveClientHost(request);
        if (requestHost == null || requestHost.isBlank()) {
            requestHost = normalizeHost(request.getServerName());
        }

        final String normalizedRequestHost = requestHost;
        boolean match = allowed.stream()
                .map(this::normalizeHost)
                .filter(Objects::nonNull)
                .anyMatch(normalizedRequestHost::equalsIgnoreCase);

        if (!match) {
            log.warn("Domain not allowed: {} for channel: {} [origin={}, referer={}, x-forwarded-host={}, host={}]",
                    normalizedRequestHost,
                    channel,
                    request.getHeader("Origin"),
                    request.getHeader("Referer"),
                    request.getHeader("X-Forwarded-Host"),
                    request.getHeader("Host"));
            throw BaseException.domainNotAllowed("Access forbidden!");
        }
    }

    private String resolveClientHost(HttpServletRequest request) {
        String[] headers = {"Origin", "Referer", "X-Forwarded-Host", "X-Original-Host", "Host"};
        for (String header : headers) {
            String value = request.getHeader(header);
            String normalized = normalizeHost(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeHost(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        // Forwarded headers may contain multiple values, first one is original client-facing host.
        int commaIdx = value.indexOf(',');
        if (commaIdx >= 0) {
            value = value.substring(0, commaIdx).trim();
        }

        // For values like "host:port", "https://host:port", or full referer URLs.
        try {
            URI uri = value.contains("://") ? URI.create(value) : URI.create("http://" + value);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri.getHost().toLowerCase();
            }
        } catch (Exception ignored) {
            // Fall through to manual parsing.
        }

        // Manual fallback for non-URI values.
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            if (end > 1) {
                return value.substring(1, end).toLowerCase();
            }
        }
        int colonIdx = value.indexOf(':');
        if (colonIdx > 0) {
            value = value.substring(0, colonIdx);
        }
        value = value.trim();
        return value.isEmpty() ? null : value.toLowerCase();
    }
}
