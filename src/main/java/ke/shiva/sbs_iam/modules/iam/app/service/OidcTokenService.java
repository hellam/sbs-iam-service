package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.modules.iam.api.request.RefreshTokenRequest;
import ke.shiva.sbs_iam.modules.iam.app.util.RequestContextExtractor;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.RefreshTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.RevokedTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.UserCategory;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.security.jwt.JwtClaimEncryption;
import ke.shiva.shivacorestarter.security.jwt.JwtClaims;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OidcTokenService {
    private static final String SESSION_VERSION_KEY = "session_version";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final LoginFlowService loginFlowService;
    private final JwtClaimEncryption jwtClaimEncryption;
    private final OrganizationUserRepository orgRepo;
    private final SessionRevocationService sessionRevocationService;
    private final SecurityEventRepository securityEventRepository;
    private final RequestContextExtractor requestContextExtractor;
    private final ImpossibleTravelDetectionService impossibleTravelDetectionService;


    // expected issuer and audience configured via properties
    @Value("${shiva.security.jwt.expected-issuer:sbs-iam}")
    private String expectedIssuer;

    @Value("${shiva.security.jwt.expected-audience:gateway-service}")
    private String expectedAudience;

    @Transactional
    public void issueTokens(Long sessionId) {
        issueTokens(sessionId, true);
    }

    @Transactional
    public void issueTokens(Long sessionId, boolean bumpSessionVersion) {
        SessionEntity session = sessionRepository.findByIdWithIamUser(sessionId).orElseThrow(
                () -> new IllegalArgumentException("Session not found with ID: " + sessionId)
        );
        if (session.getRevokedAt() != null) {
            throw BaseException.unauthorized("Session has been revoked");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw BaseException.unauthorized("Session expired");
        }
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
        // Keep a stable session_id, and invalidate prior tokens via session_version bumps.
        long sessionVersion = resolveAndMaybeBumpSessionVersion(session, bumpSessionVersion);

        // Resolve customer ID based on selected profile:
        // ORG_USER -> organization core customer ID
        // CUSTOMER -> customer profile core customer ID
        String customerId = resolveCustomerIdForToken(session, user, category);

        // Encrypt sensitive claims to prevent enumeration attacks
        String encryptedUserId = jwtClaimEncryption.encryptUserId(user.getId());
        String encryptedCustomerId = null;
        if (customerId != null && !customerId.isBlank()) {
            encryptedCustomerId = jwtClaimEncryption.encryptCustomerId(customerId);
        }

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(expectedIssuer)
                .issuedAt(now.toInstant())
                .expiresAt(now.plusSeconds(accessTokenValidity).toInstant())
                .audience(List.of(expectedAudience))
                .subject(String.valueOf(user.getPublicId()))
                .claim(JwtClaims.USER_ID, encryptedUserId)           // ENCRYPTED IAM user ID
                .claim(JwtClaims.SESSION_ID, session.getSessionId()) // Session ID for tracking
                .claim(SESSION_VERSION_KEY, sessionVersion)          // Monotonic version for scalable invalidation
                .claim(JwtClaims.CHANNEL, channel.name())
                .claim(JwtClaims.CATEGORY, category.name())
                .claim(JwtClaims.SCOPE, buildScopeFor(session))
                .id(UUID.randomUUID().toString());           // JTI

        // Add encrypted customer ID if available (for CUSTOMER category)
        if (encryptedCustomerId != null) {
            claimsBuilder.claim(JwtClaims.CUSTOMER_ID, encryptedCustomerId);
        }

        // Add device ID for device binding and fraud detection
        // Note: deviceId is already hashed, no need to encrypt again
        if (session.getDeviceId() != null) {
            claimsBuilder.claim(JwtClaims.DEVICE_ID, session.getDeviceId());
        }

        // Add profile type and ID if available (for multi-profile users)
        if (session.getProfileType() != null) {
            claimsBuilder.claim(JwtClaims.PROFILE_TYPE, session.getProfileType().name());
            if (session.getProfileType().name().equals(ProfileType.ORG_USER.name())){
                OrganizationUserEntity orgUser = orgRepo.findById(session.getProfileId()).orElseThrow(() -> new IllegalArgumentException("Organization user profile not found with ID: " + session.getProfileId()));
                claimsBuilder.claim(JwtClaims.TASK_ROLE, orgUser.getOrgRole().getTaskRole().name());
            }
        }


        if (session.getProfileId() != null) {
            String encryptedProfileId = jwtClaimEncryption.encryptProfileId(session.getProfileId());
            claimsBuilder.claim(JwtClaims.PROFILE_ID, encryptedProfileId);
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
        refreshTokenEntity.setIsActive(true);
        refreshTokenRepository.save(refreshTokenEntity);

        loginFlowService.extend(session, 30); // Extend session by 30 minutes on token issue
        // Publish current session_version to Redis for gateway-side equality checks.
        sessionRevocationService.cacheSessionVersion(session.getSessionId(), sessionVersion, session.getExpiresAt());

        setTokenHeaders(accessToken, rawRefreshToken, accessTokenValidity, refreshTokenValidity);
    }

    @Transactional
    public void refreshTokens(RefreshTokenRequest request, String deviceId) {
        log.info("Refreshing tokens for device ID {}", deviceId);
        String refreshTokenHash = HashUtil.sha256(request.getRefreshToken());
        RefreshTokenEntity oldToken = refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .orElseThrow(() -> BaseException.unauthorized("Invalid refresh token"));

        if (Boolean.FALSE.equals(oldToken.getIsActive()) || oldToken.getRevokedAt() != null) {
            // Reuse/replay of an already consumed refresh token is treated as credential theft.
            handleRefreshTokenReplay(oldToken);
            throw BaseException.unauthorized("Suspicious token activity detected. Please sign in again.");
        }
        if (oldToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
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
        if (session.getRevokedAt() != null) {
            throw BaseException.unauthorized("Session has been revoked");
        }
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw BaseException.unauthorized("Session expired");
        }

        // Evaluate impossible travel during refresh too (not only during login success).
        impossibleTravelDetectionService.enforce(session);

        //check device id matches
        if (deviceId == null || deviceId.isBlank() || !session.getDeviceId().equals(HashUtil.sha256(deviceId))) {
            log.warn("Refresh token device ID mismatch for session ID {}", session.getId());
            sessionRevocationService.revokeSessionAndDevice(session, "REFRESH_DEVICE_MISMATCH");
            saveSecurityEvent("REFRESH_DEVICE_MISMATCH", "HIGH",
                    "Refresh attempt failed due to device mismatch. Session revoked.", session, null);
            throw BaseException.unauthorized("Invalid Request");
        }


        // Issue new tokens
       issueTokens(oldToken.getSession().getId(), false);

        // Revoke the old refresh token
        oldToken.setRevokedAt(OffsetDateTime.now());
        oldToken.setRevokedReason("Replaced by new token");
        oldToken.setIsActive(false);
        refreshTokenRepository.save(oldToken);
    }

    private void handleRefreshTokenReplay(RefreshTokenEntity token) {
        SessionEntity replaySession = token.getSession();
        if (replaySession == null || replaySession.getIamUser() == null) {
            return;
        }

        // Revoke every active session for this principal as containment.
        int revokedCount = sessionRevocationService.revokeAllActiveSessionsForUser(
                replaySession.getIamUser(),
                "REFRESH_TOKEN_REUSE"
        );
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("revoked_sessions_count", revokedCount);
        metadata.put("refresh_token_id", token.getId());
        metadata.put("refresh_token_issued_at", token.getIssuedAt() != null ? token.getIssuedAt().toString() : null);
        metadata.put("refresh_token_revoked_at", token.getRevokedAt() != null ? token.getRevokedAt().toString() : null);
        saveSecurityEvent("REFRESH_TOKEN_REUSE", "CRITICAL",
                "Refresh token replay detected. Revoked all active sessions for principal.",
                replaySession,
                metadata);
    }

    private void saveSecurityEvent(String eventType,
                                   String severity,
                                   String description,
                                   SessionEntity session,
                                   Map<String, Object> metadata) {
        SecurityEventEntity event = new SecurityEventEntity();
        event.setEventType(eventType);
        event.setSeverity(severity);
        event.setDescription(description);
        event.setCreatedAt(OffsetDateTime.now());
        event.setRelatedSession(session);
        event.setIamUser(session != null ? session.getIamUser() : null);
        event.setDeviceId(session != null ? session.getDeviceId() : null);

        RequestContextExtractor.RequestContext context = requestContextExtractor.extractContext();
        if (context != null) {
            event.setIpAddress(context.getIpAddress());
            if (event.getLocationCountry() == null) {
                event.setLocationCountry(context.getLocationCountry());
            }
            if (event.getLocationCity() == null) {
                event.setLocationCity(context.getLocationCity());
            }
        }

        if (metadata != null && !metadata.isEmpty()) {
            event.setMetadata(metadata);
        }
        securityEventRepository.save(event);
    }

    private long resolveAndMaybeBumpSessionVersion(SessionEntity session, boolean bumpSessionVersion) {
        // session_version rules:
        // - First issuance initializes to 1
        // - Security/context changes bump version
        // - Refresh keeps version stable
        Map<String, Object> metadata = session.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            session.setMetadata(metadata);
        }

        long current = parseLong(metadata.get(SESSION_VERSION_KEY), 0L);
        long effective;
        if (current <= 0) {
            effective = 1L;
        } else if (bumpSessionVersion && session.getSessionType() == SessionType.LOGIN_ACTIVE) {
            effective = current + 1L;
        } else {
            effective = current;
        }

        if (effective != current) {
            metadata.put(SESSION_VERSION_KEY, effective);
            sessionRepository.save(session);
        }

        return effective;
    }

    private long parseLong(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
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

    private String resolveCustomerIdForToken(SessionEntity session, IamUserEntity user, UserCategory category) {
        if (session.getProfileType() == ProfileType.ORG_USER) {
            Long orgProfileId = session.getProfileId();
            if (orgProfileId == null) {
                log.warn("Selected organization profile is missing.");
                throw BaseException.badRequest();
            }

            OrganizationUserEntity orgUser = orgRepo.findById(orgProfileId)
                    .orElseThrow(() -> BaseException.badRequest("Organization user profile not found"));

            if (orgUser.getOrganizationParty() == null ||
                    orgUser.getOrganizationParty().getCoreCustomerId() == null ||
                    orgUser.getOrganizationParty().getCoreCustomerId().isBlank()) {
                log.warn("Organization core customer ID is missing.");
                throw BaseException.badRequest();
            }

            return orgUser.getOrganizationParty().getCoreCustomerId();
        }

        if (category != UserCategory.CUSTOMER) {
            return null;
        }

        try {
            if (user.getCustomerProfile() != null) {
                return user.getCustomerProfile().getCoreCustomerId();
            }
        } catch (Exception e) {
            log.warn("Failed to extract customer profile ID for user {}: {}", user.getId(), e.getMessage());
        }

        return null;
    }

}
