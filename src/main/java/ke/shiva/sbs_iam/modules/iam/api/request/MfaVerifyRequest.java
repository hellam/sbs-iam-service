package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MfaVerifyRequest {
    @NotNull
    private UUID flowId;

    @NotBlank
    private String code;
}
