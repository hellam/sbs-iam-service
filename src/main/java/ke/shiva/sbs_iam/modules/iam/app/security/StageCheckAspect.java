package ke.shiva.sbs_iam.modules.iam.app.security;

import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class StageCheckAspect {

    private final LoginFlowService loginFlowService;
    private final FlowIdProvider flowIdProvider;

    @Before("@annotation(requiresStage)")
    public void checkStage(RequiresStage requiresStage) {
        UUID flowId = flowIdProvider.getFlowId();
        LoginStage requiredStage = requiresStage.value();
        loginFlowService.requireStage(flowId, requiredStage);
    }
}

