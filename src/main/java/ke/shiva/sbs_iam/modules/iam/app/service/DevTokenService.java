package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.DevTokenRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.DevTokenResponse;
import ke.shiva.shivacorestarter.security.jwt.JwtClaimEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating development JWT tokens.
 * Provides easy token generation for testing and development purposes.
 *
 * <p><strong>WARNING:</strong> This service is ONLY available in 'dev' profile.
 * It bypasses normal authentication flows and should NEVER be used in production.
 *
 * @author Shiva Banking Platform
 * @version 1.0
 */
@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class DevTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtClaimEncryption jwtClaimEncryption;

    @Value("${shiva.security.downstream-jwt.expected-issuer:sbs-iam}")
    private String expectedIssuer;

    @Value("${shiva.security.downstream-jwt.expected-audience:payment-service}")
    private String expectedAudience;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Generate a development JWT token with custom claims.
     * Applies sensible defaults for any missing values.
     *
     * @param request token generation request with custom claims
     * @return generated token with metadata
     */
    public DevTokenResponse generateDevToken(DevTokenRequest request) {
        // Apply defaults
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        String customerId = request.getCustomerId() != null ? request.getCustomerId() : "100004";
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "sess_" + UUID.randomUUID().toString().substring(0, 8);
        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "DEV_DEVICE";
        String channel = request.getChannel() != null ? request.getChannel() : "INTERNET_BANKING";
        String category = request.getCategory() != null ? request.getCategory() : "CUSTOMER";
        Long expirySeconds = request.getExpirySeconds() != null ? request.getExpirySeconds() : 3600L;

        // Validate and clamp expiry
        if (expirySeconds < 60) {
            expirySeconds = 60L;
        } else if (expirySeconds > 86400) {
            expirySeconds = 86400L;
        }

        // Build scope
        String scope = request.getScope() != null ? request.getScope() : channel.toLowerCase();

        // Encrypt sensitive claims (aligned with OidcTokenService)
        String encryptedUserId = jwtClaimEncryption.encryptUserId(userId);
        String encryptedCustomerId = jwtClaimEncryption.encryptCustomerId(customerId);

        // Build JWT claims
        OffsetDateTime now = OffsetDateTime.now();
        Instant issuedAt = now.toInstant();
        Instant expiresAt = now.plusSeconds(expirySeconds).toInstant();

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(expectedIssuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .audience(List.of(expectedAudience))
                .subject("dev_user_" + userId)
                .claim("user_id", encryptedUserId)        // Encrypted (not plain userId)
                .claim("customer_id", encryptedCustomerId) // Encrypted (not plain customerId)
                .claim("session_id", sessionId)
                .claim("device_id", deviceId)
                .claim("channel", channel)
                .claim("category", category)
                .claim("scope", scope)
                .id(UUID.randomUUID().toString());

        // Add optional claims
        if (request.getProfileType() != null) {
            claimsBuilder.claim("profile_type", request.getProfileType());
        }
        if (request.getProfileId() != null) {
            claimsBuilder.claim("profile_id", request.getProfileId());
        }

        JwtClaimsSet claims = claimsBuilder.build();

        // Encode JWT
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        // Build claims map for response (show both encrypted and plain for dev convenience)
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("iss", expectedIssuer);
        claimsMap.put("aud", expectedAudience);
        claimsMap.put("sub", "dev_user_" + userId);
        claimsMap.put("user_id", encryptedUserId);         // Encrypted value in JWT
        claimsMap.put("user_id_plain", userId);            // Plain value for dev reference
        claimsMap.put("customer_id", encryptedCustomerId); // Encrypted value in JWT
        claimsMap.put("customer_id_plain", customerId);    // Plain value for dev reference
        claimsMap.put("session_id", sessionId);
        claimsMap.put("device_id", deviceId);
        claimsMap.put("channel", channel);
        claimsMap.put("category", category);
        claimsMap.put("scope", scope);
        claimsMap.put("iat", issuedAt.toString());
        claimsMap.put("exp", expiresAt.toString());

        if (request.getProfileType() != null) {
            claimsMap.put("profile_type", request.getProfileType());
        }
        if (request.getProfileId() != null) {
            claimsMap.put("profile_id", request.getProfileId());
        }

        // Generate sample curl command
        String sampleCurl = String.format(
                "curl -H \"Authorization: Bearer %s\" http://localhost:%s/api/some-endpoint",
                token.substring(0, Math.min(token.length(), 50)) + "...",
                serverPort
        );

        log.info("Generated dev token: userId={} (encrypted), customerId={} (encrypted), channel={}, expiresIn={}s",
                userId, customerId, channel, expirySeconds);

        return DevTokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expirySeconds)
                .expiresAt(expiresAt)
                .issuedAt(issuedAt)
                .claims(claimsMap)
                .sampleCurl(sampleCurl)
                .build();
    }
}
