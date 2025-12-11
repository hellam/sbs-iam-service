package ke.shiva.sbs_iam.modules.iam.domain.enums;

import lombok.Getter;

@Getter
public enum NotificationChannels {
    SMS("SMS Channel"),
    EMAIL("Email Channel"),
    WHATSAPP("Whatsapp Channel");

    private final String description;
    NotificationChannels(String description){
        this.description = description;
    }
}
