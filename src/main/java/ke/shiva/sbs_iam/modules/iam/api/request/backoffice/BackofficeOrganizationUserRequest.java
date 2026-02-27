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
public class BackofficeOrganizationUserRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "Email is required")
    private String email;

    private String username;

    private Long orgRoleId;

    private Boolean primary;
}
