package ke.shiva.sbs_iam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("IAM Service API").version("1.0"))
                .tags(List.of(
                        new Tag().name("Authentication Flow").description("Endpoints for the user authentication flow")
                ));
    }
}

