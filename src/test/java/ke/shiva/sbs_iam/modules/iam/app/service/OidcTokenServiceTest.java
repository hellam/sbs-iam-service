package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.RefreshTokenRequest;
import ke.shiva.sbs_iam.modules.iam.app.util.RequestContextExtractor;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.RefreshTokenEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RefreshTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.RevokedTokenRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.security.jwt.JwtClaimEncryption;
import ke.shiva.shivacorestarter.util.HashUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private LoginFlowService loginFlowService;
    @Mock
    private JwtClaimEncryption jwtClaimEncryption;
    @Mock
    private OrganizationUserRepository orgRepo;
    @Mock
    private SessionRevocationService sessionRevocationService;
    @Mock
    private SecurityEventRepository securityEventRepository;
    @Mock
    private RequestContextExtractor requestContextExtractor;
    @Mock
    private ImpossibleTravelDetectionService impossibleTravelDetectionService;

    @InjectMocks
    private OidcTokenService oidcTokenService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void refreshTokens_reusesCachedBundleWithinGraceWindow() throws Exception {
        ReflectionTestUtils.setField(oidcTokenService, "refreshReplayGraceSeconds", 5L);

        String rawRefreshToken = "refresh-token-123";
        String refreshTokenHash = HashUtil.sha256(rawRefreshToken);
        String deviceId = "device-abc";

        SessionEntity session = activeSession(deviceId);
        RefreshTokenEntity replayedToken = replayedInactiveToken(session, refreshTokenHash, OffsetDateTime.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHash)).thenReturn(Optional.of(replayedToken));

        Object replayBundle = newReplayBundle(
                "access-token-from-cache",
                "refresh-token-from-cache",
                300L,
                1800L,
                Instant.now().plusSeconds(5)
        );
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Object> replayCache = (ConcurrentMap<String, Object>) ReflectionTestUtils.getField(
                oidcTokenService,
                "refreshReplayCache"
        );
        replayCache.put(refreshTokenHash, replayBundle);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest, servletResponse));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(rawRefreshToken);
        oidcTokenService.refreshTokens(request, deviceId);

        assertEquals("access-token-from-cache", servletResponse.getHeader("X-Access-Token"));
        assertEquals("refresh-token-from-cache", servletResponse.getHeader("X-Refresh-Token"));
        assertEquals("300", servletResponse.getHeader("X-Access-Token-Expiry"));
        assertEquals("1800", servletResponse.getHeader("X-Refresh-Token-Expiry"));

        verify(sessionRevocationService, never()).revokeAllActiveSessionsForUser(any(), anyString());
        verify(securityEventRepository, never()).save(any());
    }

    @Test
    void refreshTokens_revokesAllSessionsForReplayOutsideGraceWindow() {
        ReflectionTestUtils.setField(oidcTokenService, "refreshReplayGraceSeconds", 5L);

        String rawRefreshToken = "refresh-token-456";
        String refreshTokenHash = HashUtil.sha256(rawRefreshToken);
        String deviceId = "device-def";

        SessionEntity session = activeSession(deviceId);
        IamUserEntity iamUser = new IamUserEntity();
        iamUser.setId(10L);
        session.setIamUser(iamUser);

        RefreshTokenEntity replayedToken = replayedInactiveToken(session, refreshTokenHash, OffsetDateTime.now().minusSeconds(30));
        when(refreshTokenRepository.findByTokenHashForUpdate(refreshTokenHash)).thenReturn(Optional.of(replayedToken));
        when(sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "REFRESH_TOKEN_REUSE")).thenReturn(3);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(rawRefreshToken);

        BaseException exception = assertThrows(BaseException.class, () -> oidcTokenService.refreshTokens(request, deviceId));
        assertTrue(exception.getMessage().contains("Suspicious token activity detected"));

        verify(sessionRevocationService).revokeAllActiveSessionsForUser(iamUser, "REFRESH_TOKEN_REUSE");
        verify(securityEventRepository).save(any());
    }

    private static SessionEntity activeSession(String deviceId) {
        SessionEntity session = new SessionEntity();
        session.setId(42L);
        session.setSessionId("session-42");
        session.setDeviceId(HashUtil.sha256(deviceId));
        session.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
        session.setRevokedAt(null);
        return session;
    }

    private static RefreshTokenEntity replayedInactiveToken(SessionEntity session,
                                                            String tokenHash,
                                                            OffsetDateTime revokedAt) {
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId(99L);
        token.setSession(session);
        token.setTokenHash(tokenHash);
        token.setIsActive(false);
        token.setRevokedAt(revokedAt);
        token.setRevokedReason("Replaced by new token");
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(20));
        return token;
    }

    private static Object newReplayBundle(String accessToken,
                                          String refreshToken,
                                          long accessTokenValidity,
                                          long refreshTokenValidity,
                                          Instant expiresAt) throws Exception {
        Class<?> bundleClass = Class.forName(OidcTokenService.class.getName() + "$ReplayTokenBundle");
        Constructor<?> constructor = bundleClass.getDeclaredConstructor(
                String.class,
                String.class,
                long.class,
                long.class,
                Instant.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(accessToken, refreshToken, accessTokenValidity, refreshTokenValidity, expiresAt);
    }
}
