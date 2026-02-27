package ke.shiva.sbs_iam.config.SecurityConfig;

import ke.shiva.sbs_iam.config.JwtRevocationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRevocationFilter jwtRevocationFilter;
    private final JwtDecoder iamJwtDecoder;
    private final JwtDecoder downstreamJwtDecoder;

    public SecurityConfig(
            JwtRevocationFilter jwtRevocationFilter,
            @Qualifier("jwtDecoder") JwtDecoder iamJwtDecoder,
            @Qualifier("downstreamJwtDecoder") JwtDecoder downstreamJwtDecoder
    ) {
        this.jwtRevocationFilter = jwtRevocationFilter;
        this.iamJwtDecoder = iamJwtDecoder;
        this.downstreamJwtDecoder = downstreamJwtDecoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)  // CORS handled at gateway level
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtRevocationFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/session/**").authenticated()
                        .requestMatchers(
                                "/device/**",
                                "/security-questions/**",
                                "/identifier/**",
                                "/login/**",
                                "/mfa/**",
                                "/refresh",
                                "/finalize/**",
                                "/password/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .authenticationManagerResolver(authenticationManagerResolver())
        );

        return http.build();
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        AuthenticationManager iamAuthManager = buildAuthenticationManager(iamJwtDecoder);
        AuthenticationManager downstreamAuthManager = buildAuthenticationManager(downstreamJwtDecoder);

        return request -> isSessionPath(request) ? downstreamAuthManager : iamAuthManager;
    }

    private AuthenticationManager buildAuthenticationManager(JwtDecoder decoder) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        return provider::authenticate;
    }

    private boolean isSessionPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return false;
        }

        return uri.startsWith("/api/v1/oauth/session/")
                || uri.startsWith("/session/");
    }
}
