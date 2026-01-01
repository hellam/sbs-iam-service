package ke.shiva.sbs_iam.modules.iam.api.request;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}

