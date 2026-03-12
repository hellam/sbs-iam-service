package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeOrganizationUserRoleUpdateRequest {

    @NotNull(message = "orgRoleId is required")
    @Positive(message = "orgRoleId must be positive")
    private Long orgRoleId;
}
