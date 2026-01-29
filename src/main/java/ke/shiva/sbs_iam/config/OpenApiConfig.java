package ke.shiva.sbs_iam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Stream;

@Configuration
public class OpenApiConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

    //Comma-separated list of server URLs
    @Value("${shiva.api-doc.servers}")
    private String servers;

    @Bean
    public OpenAPI customOpenAPI() {
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
                .servers(getServerList());
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
    public OperationCustomizer addSignatureHeaders() {
        return (operation, handlerMethod) -> {
            return operation;
        };
    }
}
