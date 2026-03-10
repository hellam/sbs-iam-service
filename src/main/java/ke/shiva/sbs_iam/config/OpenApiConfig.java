package ke.shiva.sbs_iam.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

    //Comma-separated list of server URLs
    @Value("${shiva.api-doc.servers}")
    private String servers;

    @Bean
    public OpenAPI customOpenAPI() {
        // Define JWT Bearer security scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT token for authentication. Most IAM endpoints are public for authentication flow.");

        // Define security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("BearerAuth");

        return new OpenAPI()
                .info(new Info().title("IAM Service API").version("1.0"))
                .tags(List.of(
                        new Tag().name("Authentication Flow").description("Endpoints for the user authentication flow")
                ))
                .tags(List.of(
                        new Tag().name("Identity Verification").description("Endpoints for user identity verification"),
                        new Tag().name("Password Verification").description("Endpoints for password verification"),
                        new Tag().name("MFA Verification").description("Endpoints for multi-factor authentication"),
                        new Tag().name("Authentication Flow").description("Endpoints for the user authentication flow"),
                        new Tag().name("Forgot Password").description("Endpoints for forgot password flow")
                ))
                .servers(getServerList())
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", securityScheme))
                .addSecurityItem(securityRequirement); // Apply to all operations by default
    }

    private List<Server> getServerList() {
        String[] serverUrls = servers.split(",");
        log.info("Configuring OpenAPI servers: {}", (Object) serverUrls);
        return Stream.of(serverUrls)
                .map(url -> {
                    log.info("Adding OpenAPI server: {}", url.trim());
                    return new Server().url(url.trim());
                })
                .toList();
    }

    @Bean
    public OperationCustomizer customizeOperations() {
        return (operation, handlerMethod) -> {
            // Security requirement is applied globally via customOpenAPI()
            // Most IAM endpoints are public for authentication flow
            // Internal endpoints (/internal/**) require JWT
            return operation;
        };
    }
}
