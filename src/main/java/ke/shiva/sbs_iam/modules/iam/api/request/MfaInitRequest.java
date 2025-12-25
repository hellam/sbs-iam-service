package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import lombok.Data;

import java.util.UUID;

@Data
public class MfaInitRequest {

    @NotNull
    private NotificationChannel channel;
}

