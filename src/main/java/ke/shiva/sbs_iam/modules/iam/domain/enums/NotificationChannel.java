package ke.shiva.sbs_iam.modules.iam.domain.enums;

import lombok.Getter;

@Getter
public enum NotificationChannel {
    SMS("SMS"),
    EMAIL("Email"),
    WHATSAPP("WhatsApp");

    private final String description;
    NotificationChannel(String description){
        this.description = description;
    }
}
