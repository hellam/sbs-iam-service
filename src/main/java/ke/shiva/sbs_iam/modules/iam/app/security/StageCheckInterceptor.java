package ke.shiva.sbs_iam.modules.iam.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StageCheckInterceptor implements HandlerInterceptor {

    private final LoginFlowService loginFlowService;
    private final FlowIdProvider flowIdProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequiresStage requiresStage = handlerMethod.getMethodAnnotation(RequiresStage.class);
            if (requiresStage != null) {
                UUID flowId = flowIdProvider.getFlowId();
                LoginStage requiredStage = requiresStage.value();
                loginFlowService.requireStage(flowId, requiredStage);
            }
        }
        return true;
    }
}

