package ke.shiva.microservice_template.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shiva Microservice API")
                        .description("Core API Documentation for Shiva Microservices")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Shiva Software Africa")
                                .email("support@shiva.co.ke")
                                .url("https://shiva.co.ke"))
                        .license(new License()
                                .name("Proprietary License")
                                .url("https://shiva.co.ke/license")));
    }
}
