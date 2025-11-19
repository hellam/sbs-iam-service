package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MfaInitRequest {
    @NotNull
    private UUID flowId;
}

