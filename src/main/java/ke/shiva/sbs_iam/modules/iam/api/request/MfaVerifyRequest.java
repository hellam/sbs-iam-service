package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyRequest {

    @NotBlank
    private String code;
}
