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
public class BackofficeCustomerValidationRequest {
    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    private String email;
}
