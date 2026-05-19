package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChannelIdentifierNormalizer {

    public String normalize(String identifier, Channel channel) {
        if (channel == Channel.MOBILE_BANKING) {
            return normalizeMobile(identifier);
        }
        return identifier == null ? null : identifier.trim();
    }

    private String normalizeMobile(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (!StringUtils.hasText(value)) {
            return value;
        }

        String digits = value.replaceAll("[\\s\\-+]", "");
        String countryCode = digits.length() >= 3 ? digits.substring(0, 3) : "";
        String subscriber = digits.length() > 9 ? digits.substring(digits.length() - 9) : digits;

        if ("252".equals(countryCode) || "254".equals(countryCode)) {
            return countryCode + subscriber;
        }
        return "252" + subscriber;
    }
}
