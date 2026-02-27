package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

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

    private List<@NotBlank(message = "Account number is required") String> accounts;
}
