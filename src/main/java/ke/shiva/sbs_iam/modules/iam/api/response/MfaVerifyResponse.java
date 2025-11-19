package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

import java.util.UUID;

@Data
public class MfaVerifyResponse {
    private UUID flowId;
    private boolean nextIsProfileSelection;
}
