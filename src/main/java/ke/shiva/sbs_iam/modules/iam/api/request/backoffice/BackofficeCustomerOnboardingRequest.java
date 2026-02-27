package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeCustomerOnboardingRequest {
    @NotBlank(message = "Client ID is required")
    private String clientId;

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

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Address is required")
    private String address;

    private List<@Valid BackofficeAccountRequest> accounts;
}
