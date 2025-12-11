package ke.shiva.sbs_iam.modules.iam.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordPolicyService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordPolicyValidator implements ConstraintValidator<ValidPasswordPolicy, PasswordChangeRequest> {

    private final PasswordPolicyService passwordPolicyService;
    private final LoginFlowService loginFlowService;

    @Override
    public boolean isValid(PasswordChangeRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getFlowId() == null || request.getNewPassword() == null) {
            return true; // Let other validators handle null checks
        }

        try {
            // 1. Load the session to get the context (especially the channel)
            SessionEntity session = loginFlowService.requireAtLeast(request.getFlowId(), LoginStage.IDENTIFIER_OK);

            // 2. Delegate to the existing service
            passwordPolicyService.validatePasswordChange(session, request.getOldPassword(), request.getNewPassword());

        } catch (Exception e) {
            // If validation fails, the service throws an exception. We catch it and build a constraint violation.
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage())
                   .addPropertyNode("newPassword") // Attach the error to the 'newPassword' field
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}
