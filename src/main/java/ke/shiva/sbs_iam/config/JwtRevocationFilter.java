package ke.shiva.sbs_iam.config;

import ke.shiva.sbs_iam.modules.iam.infra.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRevocationFilter extends OncePerRequestFilter {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtDecoder jwtDecoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtDecoder.decode(token).getId();
                if (revokedTokenRepository.findByJti(jti).isPresent()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                    return;
                }
            } catch (Exception e) {
                // Ignore invalid tokens, they will be caught by the standard security filter
            }
        }
        filterChain.doFilter(request, response);
    }
}

