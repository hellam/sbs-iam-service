package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for development JWT token generation.
 * Contains the generated token and metadata about the token.
 *
 * @author Shiva Banking Platform
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevTokenResponse {

    /**
     * The generated JWT token.
     * Use this in the Authorization header: "Bearer {token}"
     */
    private String token;

    /**
     * Token type (always "Bearer").
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token expiry time in seconds.
     */
    private Long expiresIn;

    /**
     * Absolute expiration timestamp (ISO-8601).
     */
    private Instant expiresAt;

    /**
     * Token issuance timestamp (ISO-8601).
     */
    private Instant issuedAt;

    /**
     * JWT claims included in the token.
     */
    private Map<String, Object> claims;

    /**
     * Sample curl command for using the token.
     */
    private String sampleCurl;

    /**
     * Warning message (reminds developers this is for dev only).
     */
    @Builder.Default
    private String warning = "⚠️ This token is for DEVELOPMENT use only. Never use in production!";
}
