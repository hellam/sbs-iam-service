package ke.shiva.sbs_iam.modules.iam.domain.enums.identity;

import lombok.Getter;

@Getter
public enum Channel {
    INTERNET_BANKING("Internet Banking"),
    MOBILE_BANKING("Mobile Banking"),
    BACKOFFICE("Backoffice"),
    AGENT_APP("Agent App"),
    MERCHANT_PORTAL("Merchant Portal"),
    API_ACCESS("API Access");

    private final String description;

    Channel(String description) {
        this.description = description;
    }
}
