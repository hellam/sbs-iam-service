package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionPasswordContextResponse {
    private String sessionId;
    private String publicKey;
}

