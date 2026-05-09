package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeSessionPolicyUpdateRequest {
    private Integer inactivityTimeoutSeconds;
    private Integer warningCountdownSeconds;
}
