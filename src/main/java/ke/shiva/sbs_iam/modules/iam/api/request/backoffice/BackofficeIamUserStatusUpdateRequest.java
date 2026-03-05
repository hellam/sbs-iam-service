package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeIamUserStatusUpdateRequest {

    @NotNull(message = "status is required")
    private IamStatus status;
}
