package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.api.validation.ValidForgotPasswordPolicy;
import lombok.Data;

import java.util.Objects;

@Data
@ValidForgotPasswordPolicy(message = "Password does not meet the policy requirements.")
public class ForgotPasswordResetRequest {
    @NotBlank(message = "New password is required")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @AssertTrue(message = "Password confirmation does not match")
    private boolean isPasswordConfirmed() {
        return Objects.equals(this.newPassword, this.confirmPassword);
    }
}
