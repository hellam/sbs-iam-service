package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PasswordChangeRequest {

    @NotNull
    private UUID flowId;

    private String oldPassword; // optional depending on policy

    @NotBlank
    private String newPassword;

    @NotBlank
    private String newPasswordConfirmation;
}
