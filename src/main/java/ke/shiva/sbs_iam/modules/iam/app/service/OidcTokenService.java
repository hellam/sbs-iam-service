package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.modules.iam.api.request.RefreshTokenRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.RefreshTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.DeviceEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.RevokedTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.UserCategory;
import ke.shiva.sbs_iam.modules.iam.infra.repository.DeviceRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RefreshTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RevokedTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.security.jwt.JwtClaimEncryption;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.SecureRandomStringGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OidcTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final OtpService otpService;
    private final LoginFlowService loginFlowService;
    private final DeviceRepository deviceRepository;
    private final JwtClaimEncryption jwtClaimEncryption;


    // expected issuer and audience configured via properties
    @Value("${shiva.security.jwt.expected-issuer:sbs-iam}")
    private String expectedIssuer;

    @Value("${shiva.security.jwt.expected-audience:gateway-service}")
    private String expectedAudience;

    @Transactional
    public void issueTokens(Long sessionId) {
        SessionEntity session = sessionRepository.findByIdWithIamUser(sessionId).orElseThrow(
                () -> new IllegalArgumentException("Session not found with ID: " + sessionId)
        );
        IamUserEntity user = session.getIamUser();
        Channel channel = session.getChannel();
        UserCategory category = null;
        switch (channel) {
            case INTERNET_BANKING, MOBILE_BANKING -> category = UserCategory.CUSTOMER;
            case BACKOFFICE -> category = UserCategory.EMPLOYEE;
            default -> throw new IllegalArgumentException("Unsupported channel: " + channel);
        }

        OffsetDateTime now = OffsetDateTime.now();
        long accessTokenValidity = 300L; // 5 minutes
        long refreshTokenValidity = 1800L; // 30 minutes

        // Extract customer ID from user profile
        String customerId = extractCustomerId(user, category);

        // Encrypt sensitive claims to prevent enumeration attacks
        String encryptedUserId = jwtClaimEncryption.encryptUserId(user.getId());
        String encryptedCustomerId = jwtClaimEncryption.encryptCustomerId(customerId);

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(expectedIssuer)
                .issuedAt(now.toInstant())
                .expiresAt(now.plusSeconds(accessTokenValidity).toInstant())
                .audience(List.of(expectedAudience))
                .subject(String.valueOf(user.getPublicId()))
                .claim("user_id", encryptedUserId)           // ENCRYPTED IAM user ID
                .claim("session_id", session.getSessionId()) // Session ID for tracking
                .claim("channel", channel.name())
                .claim("category", category.name())
                .claim("scope", buildScopeFor(session))
                .id(UUID.randomUUID().toString());           // JTI

        // Add encrypted customer ID if available (for CUSTOMER category)
        if (encryptedCustomerId != null) {
            claimsBuilder.claim("customer_id", encryptedCustomerId);
        }

        // Add device ID for device binding and fraud detection
        // Note: deviceId is already hashed, no need to encrypt again
        if (session.getDeviceId() != null) {
            claimsBuilder.claim("device_id", session.getDeviceId());
        }

        // Add profile type and ID if available (for multi-profile users)
        if (session.getProfileType() != null) {
            claimsBuilder.claim("profile_type", session.getProfileType().name());
        }
        if (session.getProfileId() != null) {
            String encryptedProfileId = jwtClaimEncryption.encryptProfileId(session.getProfileId());
            claimsBuilder.claim("profile_id", encryptedProfileId);
        }

        JwtClaimsSet accessClaims = claimsBuilder.build();

        log.debug("Generated JWT with encrypted claims for user publicId: {}", user.getPublicId());

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).getTokenValue();

        String rawRefreshToken = SecureRandomStringGen.generate();
        String refreshTokenHash = HashUtil.sha256(rawRefreshToken);

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setSession(session);
        refreshTokenEntity.setTokenHash(refreshTokenHash);
        refreshTokenEntity.setIssuedAt(now);
        refreshTokenEntity.setExpiresAt(now.plusSeconds(refreshTokenValidity));
        refreshTokenRepository.save(refreshTokenEntity);

        loginFlowService.extend(session, 30); // Extend session by 30 minutes on token issue

        setTokenHeaders(accessToken, rawRefreshToken, accessTokenValidity, refreshTokenValidity);
    }

    @Transactional
    public void refreshTokens(RefreshTokenRequest request, String deviceId) {
        log.info("Refreshing tokens for device ID {}", deviceId);
        String refreshTokenHash = HashUtil.sha256(request.getRefreshToken());
        RefreshTokenEntity oldToken = refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> BaseException.unauthorized("Invalid refresh token"));

        if (oldToken.getRevokedAt() != null || oldToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw BaseException.unauthorized("Refresh token is revoked or expired");
        }

        // Revoke the old access token
        try {
            HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String authHeader = servletRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Jwt decodedJwt = jwtDecoder.decode(token);
                String jti = decodedJwt.getId();
                assert decodedJwt.getExpiresAt() != null;
                OffsetDateTime expiry = OffsetDateTime.ofInstant(decodedJwt.getExpiresAt(), java.time.ZoneId.systemDefault());

                RevokedTokenEntity revokedToken = new RevokedTokenEntity();
                revokedToken.setJti(jti);
                revokedToken.setExpiryDate(expiry);
                revokedTokenRepository.save(revokedToken);
            }
        } catch (Exception e) {
            // Ignore if the token is invalid, it can't be used anyway
        }

        //get session and validate
        SessionEntity session = oldToken.getSession();
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw BaseException.unauthorized("Session expired");
        }

        //check device id matches
        if (!session.getDeviceId().equals(HashUtil.sha256(deviceId))) {
            //TODO: Log possible token theft attempt
            log.warn("Refresh token device ID mismatch for session ID {}", session.getId());
            DeviceEntity device = deviceRepository.findByDeviceIdAndActiveTrue(deviceId).orElseThrow(
                    () -> BaseException.badRequest("Invalid Request")
            );

            device.setRiskLevel("HIGH");
            device.setRiskScore(100);
            deviceRepository.save(device);

            //TODO: Revoke all tokens associated with this session
            //TODO: Send event to GW to block further requests from this session

            throw BaseException.badRequest("Invalid Request");
        }


        // Issue new tokens
       issueTokens(oldToken.getSession().getId());

        // Revoke the old refresh token
        oldToken.setRevokedAt(OffsetDateTime.now());
        oldToken.setRevokedReason("Replaced by new token");
        refreshTokenRepository.save(oldToken);
    }

    private String buildScopeFor(SessionEntity session) {
        // TODO: build scopes from roles/permissions (RBAC)
        // For now: return channel as scope
        return session.getChannel().name().toLowerCase();
    }

//    private void setTokenCookies(String accessToken, String refreshToken, long accessTokenValidity, long refreshTokenValidity) {
//        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
//        if (response == null) return;
//
//        ResponseCookie accessCookie = ResponseCookie.from(SecurityConstants.Cookies.ACCESS_TOKEN_NAME, accessToken)
//                .httpOnly(true)
//                .secure(cookieSecure)
//                .path("/")
//                .maxAge(Duration.ofSeconds(accessTokenValidity))
//                .sameSite(cookieSameSite)
//                .build();
//
//        ResponseCookie refreshCookie = ResponseCookie.from(SecurityConstants.Cookies.REFRESH_TOKEN_NAME, refreshToken)
//                .httpOnly(true)
//                .secure(cookieSecure)
//                .path("/")
//                .maxAge(Duration.ofSeconds(refreshTokenValidity))
//                .sameSite(cookieSameSite)
//                .build();
//
//        response.addHeader("Set-Cookie", accessCookie.toString());
//        response.addHeader("Set-Cookie", refreshCookie.toString());
//    }

    private void setTokenHeaders(String accessToken, String refreshToken, long accessTokenValidity, long refreshTokenValidity) {
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();

        if (response == null) return;

        response.setHeader("X-Access-Token", accessToken);
        response.setHeader("X-Access-Token-Expiry", String.valueOf(accessTokenValidity));

        response.setHeader("X-Refresh-Token", refreshToken);
        response.setHeader("X-Refresh-Token-Expiry", String.valueOf(refreshTokenValidity));
    }

    /**
     * Extract core banking customer ID from IAM user profile.
     * Only applicable for CUSTOMER category users.
     *
     * @param user the IAM user
     * @param category the user category
     * @return customer ID or null if not applicable
     */
    private String extractCustomerId(IamUserEntity user, UserCategory category) {
        if (category != UserCategory.CUSTOMER) {
            return null;
        }

        try {
            // Customer profile is eagerly loaded with user
            if (user.getCustomerProfile() != null) {
                return user.getCustomerProfile().getCoreCustomerId();
            }
        } catch (Exception e) {
            log.warn("Failed to extract customer ID for user {}: {}", user.getId(), e.getMessage());
        }

        return null;
    }

}
