package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeChannelAccessRequest {

    @NotNull(message = "enabled is required")
    private Boolean enabled;
}
