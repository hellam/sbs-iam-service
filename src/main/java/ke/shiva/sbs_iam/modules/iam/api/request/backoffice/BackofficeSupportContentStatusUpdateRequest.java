package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeSupportContentStatusUpdateRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
