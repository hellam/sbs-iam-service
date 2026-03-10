package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeAccessLockRequest {

    @NotNull(message = "blocked is required")
    private Boolean blocked;
}
