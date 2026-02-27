package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeEmployeeValidationRequest {
    @NotBlank(message = "Staff number is required")
    private String staffNo;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "Email is required")
    private String email;

    private String username;
}
