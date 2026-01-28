package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.api.request.RefreshTokenRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.RefreshTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.DeviceEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.RevokedTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.UserCategory;
import ke.shiva.sbs_iam.modules.iam.infra.repository.DeviceRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RefreshTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RevokedTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.SecureRandomStringGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import java.time.Duration;
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

    @Value("${shiva.security.cookies.secure:true}")
    private boolean cookieSecure;

    @Value("${shiva.security.cookies.same-site:Lax}")
    private String cookieSameSite;


    // expected issuer and audience configured via properties
    @Value("${shiva.security.jwt.expected-issuer:sbs-iam}")
    private String expectedIssuer;

    @Value("${shiva.security.jwt.expected-audience:gateway-service}")
    private String expectedAudience;

    @Transactional
    public OidcTokenResponse issueTokens(Long sessionId) {
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

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(expectedIssuer)
                .issuedAt(now.toInstant())
                .expiresAt(now.plusSeconds(accessTokenValidity).toInstant())
                .audience(List.of(expectedAudience))
                .subject(String.valueOf(user.getPublicId()))
                .claim("channel", channel.name())
                .claim("category", category.name())
                .claim("scope", buildScopeFor(session))
                .id(UUID.randomUUID().toString()) // JTI
                .build();

        if (session.getProfileType() != null) {
            accessClaims = JwtClaimsSet.from(accessClaims).claim("profile_type", session.getProfileType()).build();
        }
        if (session.getProfileId() != null) {
            accessClaims = JwtClaimsSet.from(accessClaims).claim("profile_id", session.getProfileId()).build();
        }

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

        OidcTokenResponse resp = new OidcTokenResponse();
//        resp.setAccessToken(accessToken);
//        resp.setRefreshToken(rawRefreshToken);
        resp.setExpiresIn(accessTokenValidity);
//        resp.setIdToken(buildIdToken(session, user, now, accessTokenValidity));
        return resp;
    }

    @Transactional
    public OidcTokenResponse refreshTokens(RefreshTokenRequest request, String deviceId) {
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
        OidcTokenResponse response = issueTokens(oldToken.getSession().getId());

        // Revoke the old refresh token
        oldToken.setRevokedAt(OffsetDateTime.now());
        oldToken.setRevokedReason("Replaced by new token");
        refreshTokenRepository.save(oldToken);

        return response;
    }

    private String buildIdToken(SessionEntity session, IamUserEntity user, OffsetDateTime now, long expiresIn) {
        JwtClaimsSet idClaims = JwtClaimsSet.builder()
                .issuer("sbs-iam")
                .issuedAt(now.toInstant())
                .expiresAt(now.plusSeconds(expiresIn).toInstant())
                .subject(String.valueOf(user.getPublicId()))
                .claim("name", user.getParty().getPerson().getFullName())
                .claim("email", otpService.getContactForNotificationChannel(session, NotificationChannel.EMAIL).getContactValue())
                .claim("phone_number", otpService.getContactForNotificationChannel(session, NotificationChannel.SMS).getContactValue())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(idClaims)).getTokenValue();
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

}
