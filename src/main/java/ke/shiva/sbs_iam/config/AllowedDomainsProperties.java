package ke.shiva.sbs_iam.config;

import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;


@ConfigurationProperties(prefix = "security.allowed-domains")
public class AllowedDomainsProperties {

    /**
     * Map of ChannelEnum -> list of allowed hostnames (or patterns).
     * Example in YAML:
     * security:
     *   allowed-domains:
     *     map:
     *       BACKOFFICE:
     *         - "backoffice.bank.com"
     *         - "admin.bank.com"
     *       INTERNET_BANKING:
     *         - "ib.bank.com"
     */
    private Map<Channel, List<String>> map = new EnumMap<>(Channel.class);

    public Map<Channel, List<String>> getMap() {
        return map;
    }

    public void setMap(Map<Channel, List<String>> map) {
        this.map = map;
    }
}
