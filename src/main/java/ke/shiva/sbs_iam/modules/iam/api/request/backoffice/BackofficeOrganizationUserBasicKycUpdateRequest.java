package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeOrganizationUserBasicKycUpdateRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;
}
