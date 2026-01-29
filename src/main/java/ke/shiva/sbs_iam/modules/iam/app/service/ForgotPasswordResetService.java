package ke.shiva.sbs_iam.modules.iam.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.shiva.sbs_iam.modules.iam.api.request.ForgotPasswordResetRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.ForgotPasswordRequirements;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordResetService {

    private final ForgotPasswordFlowService flowService;
    private final PasswordUpdateService passwordUpdateService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(ForgotPasswordResetRequest request, UUID flowId) {
        // Get and validate session - must be at the correct stage based on requirements
        SessionEntity session = flowService.requireAtLeast(flowId, LoginStage.FP_IDENTIFIER_OK);

        // Get requirements from metadata
        ForgotPasswordRequirements requirements = objectMapper.convertValue(
                session.getMetadata().get("requirements"),
                ForgotPasswordRequirements.class
        );

        // Validate that all required steps have been completed
        validateRequiredStepsCompleted(session, requirements);

        // Note: Password confirmation and policy validation are handled by @ValidForgotPasswordPolicy annotation

        // Update password
        IamUserEntity user = session.getIamUser();
        passwordUpdateService.updatePassword(user, request.getNewPassword(), session.getChannel());

        // Complete flow
        flowService.complete(session);

        log.info("Password reset completed successfully for user ID: {}", user.getId());
    }

    private void validateRequiredStepsCompleted(SessionEntity session, ForgotPasswordRequirements requirements) {
        LoginStage currentStage = session.getStatus();

        // If security questions were required, must be at least FP_SECURITY_QUESTIONS_OK
        if (requirements.isSecurityQuestionsRequired() &&
            currentStage.ordinal() < LoginStage.FP_SECURITY_QUESTIONS_OK.ordinal()) {
            log.warn("Security questions verification required but not completed for session: {}", session.getSessionId());
            throw BaseException.badRequest();
        }

        // If MFA was required, must be at FP_MFA_OK
        if (requirements.isMfaRequired() && currentStage != LoginStage.FP_MFA_OK) {
            log.warn("MFA verification required but not completed for session: {}", session.getSessionId());
            throw BaseException.badRequest();
        }

        // If no security questions and no MFA, must be at least FP_IDENTIFIER_OK
        if (!requirements.isSecurityQuestionsRequired() && !requirements.isMfaRequired() &&
            currentStage.ordinal() < LoginStage.FP_IDENTIFIER_OK.ordinal()) {
            throw BaseException.invalidStage();
        }
    }
}
