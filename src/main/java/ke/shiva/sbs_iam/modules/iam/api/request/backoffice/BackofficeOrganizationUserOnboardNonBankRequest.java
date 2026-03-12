package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BackofficeOrganizationUserOnboardNonBankRequest {
    @NotBlank(message = "customerId is required (internal non-bank customer ID, not core banking customer ID)")
    private String customerId;

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    private String idNumber;
    private String passport;

    @NotNull(message = "orgRoleId is required")
    @Positive(message = "orgRoleId must be positive")
    private Long orgRoleId;

    @NotEmpty(message = "clientAccountIds is required")
    private List<@NotBlank(message = "clientAccountId cannot be blank") String> clientAccountIds;
}
