package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.UserCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OidcTokenService {

    private final JwtEncoder jwtEncoder;

    public OidcTokenResponse issueTokens(SessionEntity session) {

        IamUserEntity user = session.getIamUser();
        Channel channel = session.getChannel();
        UserCategory category = user.getUserCategory();

        OffsetDateTime now = OffsetDateTime.now();
        long expiresIn = 300L;

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer("sbs-iam")
                .issuedAt(now.toInstant())
                .expiresAt(now.plusSeconds(expiresIn).toInstant())
                .subject(String.valueOf(user.getPublicId()))
                .claim("channel", channel.name())
                .claim("category", category.name())
                .claim("profile_type", session.getProfileType())
                .claim("profile_id", session.getProfileId())
                .claim("scope", buildScopeFor(session))
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).getTokenValue();

        // You can later add proper refresh token & ID token handling
        OidcTokenResponse resp = new OidcTokenResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(null); // TODO: implement refresh flow
        resp.setExpiresIn(expiresIn);
        return resp;
    }

    private String buildScopeFor(SessionEntity session) {
        // TODO: build scopes from roles/permissions (RBAC)
        // For now: return channel as scope
        return session.getChannel().name().toLowerCase();
    }
}

