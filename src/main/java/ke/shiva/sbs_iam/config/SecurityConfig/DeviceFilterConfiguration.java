package ke.shiva.sbs_iam.config.SecurityConfig;

import ke.shiva.sbs_iam.modules.iam.app.security.DeviceIdFilter;
import ke.shiva.sbs_iam.modules.iam.app.security.DeviceValidationMode;
import ke.shiva.sbs_iam.modules.iam.app.service.DeviceIdValidator;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceFilterConfiguration {

//    @Bean
//    public FilterRegistrationBean<DeviceIdFilter> existenceOnlyDeviceFilter(DeviceIdValidator validator,
//                                                                            FlowIdProvider flowIdProvider) {
//        DeviceIdFilter filter = new DeviceIdFilter(validator, flowIdProvider,
//                DeviceValidationMode.EXISTENCE_ONLY, true);
//        FilterRegistrationBean<DeviceIdFilter> registration = new FilterRegistrationBean<>(filter);
//        registration.setUrlPatterns(java.util.List.of(
//                "/api/v1/oauth/identifier/backoffice",
//                "/api/v1/oauth/identifier/mobile",
//                "/api/v1/oauth/identifier/internet-banking"
//        ));
//        registration.setOrder(2);
//        return registration;
//    }

    @Bean
    public FilterRegistrationBean<DeviceIdFilter> sessionBoundDeviceFilter(DeviceIdValidator validator,
                                                                           FlowIdProvider flowIdProvider) {
        DeviceIdFilter filter = new DeviceIdFilter(validator, flowIdProvider,
                DeviceValidationMode.SESSION_BOUND, true);
        FilterRegistrationBean<DeviceIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setUrlPatterns(java.util.List.of(
                "/api/v1/oauth/password",
                "/api/v1/oauth/mfa/initiate",
                "/api/v1/oauth/mfa/verify",
                "/api/v1/oauth/password/change",
                "/api/v1/oauth/security-questions"
        ));
        registration.setOrder(3);
        return registration;
    }
}
