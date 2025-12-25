package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordLoginRequest {


    @NotBlank
    private String identifier;

    @NotBlank
    private String password;
}

