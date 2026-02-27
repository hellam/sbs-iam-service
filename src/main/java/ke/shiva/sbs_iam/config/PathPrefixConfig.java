package ke.shiva.sbs_iam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PathPrefixConfig implements WebMvcConfigurer {

    private static final String BACKOFFICE_PREFIX = "/session/backoffice";
    private static final String PUBLIC_PREFIX = "/session/ext";
    private static final String INTERNAL_PREFIX = "/internal";

    private static final String BACKOFFICE_PACKAGE = "ke.shiva.sbs_iam.modules.iam.api.controller.backoffice";
    private static final String BACKOFFICE_PACKAGE1 = "ke.shiva.sbs_iam.modules.reference.api.controller.backoffice";

    private static final String PUBLIC_PACKAGE = "ke.shiva.sbs_iam.modules.iam.api.controller.external";
    private static final String PUBLIC_PACKAGE1 = "ke.shiva.sbs_iam.modules.reference.api.controller.external";

    private static final String INTERNAL_PACKAGE = "ke.shiva.sbs_iam.modules.iam.api.controller.internal";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {

        configurer.addPathPrefix(
                BACKOFFICE_PREFIX,
                handlerType -> handlerType != null
                        && (handlerType.getPackageName().startsWith(BACKOFFICE_PACKAGE)
                        || handlerType.getPackageName().startsWith(BACKOFFICE_PACKAGE1))
        );

        configurer.addPathPrefix(
                PUBLIC_PREFIX,
                handlerType -> handlerType != null
                        && (handlerType.getPackageName().startsWith(PUBLIC_PACKAGE)
                        || handlerType.getPackageName().startsWith(PUBLIC_PACKAGE1))
        );

        configurer.addPathPrefix(
                INTERNAL_PREFIX,
                handlerType -> handlerType != null
                        && handlerType.getPackageName().startsWith(INTERNAL_PACKAGE)
        );
    }
}
