package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

@Data
public class OidcTokenResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String tokenType = "Bearer";
    private String idToken;
}
