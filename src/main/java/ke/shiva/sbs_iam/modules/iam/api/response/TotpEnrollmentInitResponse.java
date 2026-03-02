package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotpEnrollmentInitResponse {
    private String secretKey;
    private String otpauthUri;
    private String issuer;
    private String accountName;
    private int digits;
    private int periodSeconds;
}
