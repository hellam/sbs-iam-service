package ke.shiva.sbs_iam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.tags.Tag;
import ke.shiva.shivacorestarter.security.RequireSignature;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenApiConfig.class);

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("IAM Service API").version("1.0"))
                .tags(List.of(
                        new Tag().name("Authentication Flow").description("Endpoints for the user authentication flow")
                ));
    }

    @Bean
    public OperationCustomizer addSignatureHeaders() {
        return (operation, handlerMethod) -> {
            RequireSignature requireSignature = handlerMethod.getMethodAnnotation(RequireSignature.class);
            if (requireSignature != null && !requireSignature.skip()) {
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("X-Signature")
                        .description("Request signature (HMAC-SHA256)")
                        .required(true));
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("X-Timestamp")
                        .description("Unix timestamp of the request")
                        .required(true));
            }
            return operation;
        };
    }
}
