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
public class BackofficeClientLookupRequest {
    @NotBlank(message = "Client ID is required")
    private String clientId;

    // Organization onboarding only (optional; defaults to false when omitted)
    private Boolean isSme;
}
