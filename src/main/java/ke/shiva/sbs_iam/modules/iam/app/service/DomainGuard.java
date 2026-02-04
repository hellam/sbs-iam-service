package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.config.AllowedDomainsProperties;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

        String host =
                request.getHeader("X-Forwarded-Host") != null
                        ? request.getHeader("X-Forwarded-Host")
                        : request.getServerName();

        boolean match = allowed.stream().anyMatch(host::equalsIgnoreCase);

        if (!match) {
            log.warn("Domain not allowed: {} for channel: {}", host, channel);
            throw BaseException.domainNotAllowed("Access forbidden!");
        }
    }
}
