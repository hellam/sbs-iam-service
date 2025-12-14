package ke.shiva.sbs_iam.modules.iam.domain.enums;

import lombok.Getter;

@Getter
public enum NotificationChannel {
    SMS("SMS Channel"),
    EMAIL("Email Channel"),
    WHATSAPP("Whatsapp Channel");

    private final String description;
    NotificationChannel(String description){
        this.description = description;
    }
}
