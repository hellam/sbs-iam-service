package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import ke.shiva.sbs_iam.modules.iam.domain.enums.TransactionMfaAction;
import lombok.Data;

@Data
public class MfaVerifyRequest {

    @NotBlank
    private String code;

    /**
     * Optional action context used by internal transaction flows.
     * Login MFA verification can leave this null.
     */
    private TransactionMfaAction action;
}
