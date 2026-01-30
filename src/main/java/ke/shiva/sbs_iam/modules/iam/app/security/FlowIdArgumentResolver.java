package ke.shiva.sbs_iam.modules.iam.app.security;

import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

public class FlowIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(FlowId.class) != null;
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String flowId = webRequest.getHeader(SecurityConstants.Headers.FLOW_ID_HEADER);
        if (flowId == null) {
            return null;
        }
        return UUID.fromString(flowId);
    }
}

