package ke.shiva.sbs_iam.config.SecurityConfig;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Slf4j
@Configuration
public class JwtConfig {

    @Value("${shiva.security.jwt.public-key}")
    private String jwtPublicKeyBase64;

    @Value("${shiva.security.jwt.private-key}")
    private String jwtPrivateKeyBase64;

    @Value("${shiva.security.jwt.expected-issuer:sbs-iam}")
    private String iamExpectedIssuer;

    @Value("${shiva.security.jwt.expected-audience:gateway-service}")
    private String iamExpectedAudience;

    @Value("${shiva.security.downstream-jwt.public-key}")
    private String downstreamJwtPublicKeyBase64;

    @Value("${shiva.security.downstream-jwt.expected-issuer:https://gateway.service}")
    private String downstreamExpectedIssuer;

    @Value("${shiva.security.downstream-jwt.expected-audience:iam-service}")
    private String downstreamExpectedAudience;

    @Bean
    public RSAPublicKey rsaPublicKey() {
        try {
            log.info("Loading JWT RSA public key from environment: shiva.security.jwt.public-key");

            if (jwtPublicKeyBase64 == null || jwtPublicKeyBase64.trim().isEmpty()) {
                throw new IllegalStateException("JWT public key is not configured. Set shiva.security.jwt.public-key");
            }

            String clean = jwtPublicKeyBase64
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", ""); // remove all whitespace & newlines

            byte[] decoded = Base64.getDecoder().decode(clean);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);

            log.info("✅ JWT RSA public key loaded successfully from environment");
            return publicKey;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to load JWT RSA public key: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot start application: JWT public key is invalid or not configured", e);
        }
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() {
        try {
            log.info("Loading JWT RSA private key from environment: shiva.security.jwt.private-key");

            if (jwtPrivateKeyBase64 == null || jwtPrivateKeyBase64.trim().isEmpty()) {
                throw new IllegalStateException("JWT private key is not configured. Set shiva.security.jwt.private-key");
            }

            String clean = jwtPrivateKeyBase64
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(clean);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);

            log.info("✅ JWT RSA private key loaded successfully from environment");
            return privateKey;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to load JWT RSA private key: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot start application: JWT private key is invalid or not configured", e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        log.info("Initializing JWT Encoder with RSA key pair");

        // Validate keys
        log.info("=== JWT Key Configuration Validated ===");
        log.info("JWT Public Key loaded: {} bit", publicKey.getModulus().bitLength());
        log.info("JWT Private Key loaded: {} bit", privateKey.getModulus().bitLength());
        log.info("JWT Key ID: sbs-iam-kid-1");

        if (publicKey.getModulus().bitLength() < 2048) {
            log.warn("⚠️  JWT key size is less than 2048 bits. Consider using a stronger key for production.");
        } else {
            log.info("✅ JWT keys meet minimum security requirements");
        }

        // Verify keys are a valid pair
        if (!publicKey.getModulus().equals(privateKey.getModulus())) {
            throw new IllegalStateException("JWT public and private keys do not form a valid keypair!");
        }
        log.info("✅ JWT keypair validation successful");

        JWK jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("sbs-iam-kid-1")
                .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
        log.info("✅ JWT Encoder initialized successfully");
        return encoder;
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        log.info("Initializing JWT Decoder with RSA public key");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(buildValidator(iamExpectedIssuer, iamExpectedAudience));
        log.info("✅ JWT Decoder initialized successfully");
        return decoder;
    }

    @Bean("downstreamJwtDecoder")
    public JwtDecoder downstreamJwtDecoder() {
        log.info("Initializing downstream JWT Decoder with gateway RSA public key");
        RSAPublicKey publicKey = parsePublicKey(downstreamJwtPublicKeyBase64, "shiva.security.downstream-jwt.public-key");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(buildValidator(downstreamExpectedIssuer, downstreamExpectedAudience));
        log.info("✅ Downstream JWT Decoder initialized successfully");
        return decoder;
    }

    private RSAPublicKey parsePublicKey(String keyValue, String propertyName) {
        try {
            if (keyValue == null || keyValue.trim().isEmpty()) {
                throw new IllegalStateException("JWT public key is not configured. Set " + propertyName);
            }

            String clean = keyValue
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(clean);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to parse RSA public key from {}: {}", propertyName, e.getMessage(), e);
            throw new IllegalStateException("Cannot start application: invalid RSA public key for " + propertyName, e);
        }
    }

    private OAuth2TokenValidator<Jwt> buildValidator(String expectedIssuer, String expectedAudience) {
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(expectedIssuer);
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>("aud",
                aud -> aud != null && aud.contains(expectedAudience));
        return new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);
    }
}
