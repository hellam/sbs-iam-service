package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.api.validation.ValidPasswordPolicy;
import lombok.Data;

import java.util.Objects;
import java.util.UUID;

@Data
@ValidPasswordPolicy(message = "Password does not meet the policy requirements.")
public class PasswordChangeRequest {

    private String oldPassword; // optional depending on policy

    @NotBlank
    private String newPassword;

    @NotBlank
    private String newPasswordConfirmation;

    @AssertTrue(message = "Password confirmation does not match")
    private boolean isNewPasswordConfirmed() {
        return Objects.equals(this.newPassword, this.newPasswordConfirmation);
    }
}
