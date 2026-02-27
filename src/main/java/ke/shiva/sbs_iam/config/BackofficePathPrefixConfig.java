package ke.shiva.sbs_iam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BackofficePathPrefixConfig implements WebMvcConfigurer {

    private static final String BACKOFFICE_PREFIX = "/session/backoffice";
    private static final String BACKOFFICE_PACKAGE = "ke.shiva.sbs_iam.modules.iam.api.controller.backoffice";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                BACKOFFICE_PREFIX,
                handlerType -> handlerType != null
                        && handlerType.getPackageName().startsWith(BACKOFFICE_PACKAGE)
        );
    }
}
