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
public class BackofficeOrganizationOnboardingRequest {
    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Legal name is required")
    private String legalName;

    private String displayName;

    private String registrationNo;

    @NotBlank(message = "Customer segment is required")
    private String customerSegment;

    private Boolean smeMode;

    @NotBlank(message = "Country is required")
    private String country;

    private String address;

    private String city;

    private String companyPhone;

    private String companyEmail;

    private String contactPersonName;

    private String contactPersonEmail;

    private String contactPersonPhone;

    private List<@Valid BackofficeAccountRequest> accounts;

    private BackofficeOrganizationUserRequest orgUser;
}
