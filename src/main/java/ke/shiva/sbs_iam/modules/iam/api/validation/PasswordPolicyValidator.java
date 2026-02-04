package ke.shiva.sbs_iam.modules.iam.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordManager;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordPolicyService;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordVerifier;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordPolicyValidator implements ConstraintValidator<ValidPasswordPolicy, PasswordChangeRequest> {

    private final PasswordPolicyService passwordPolicyService;
    private final LoginFlowService loginFlowService;
    private final FlowIdProvider flowIdProvider;
    private final PasswordManager passwordManager;
    private final PasswordVerifier passwordVerifier;

    @Override
    public boolean isValid(PasswordChangeRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getNewPassword() == null) {
            return true; // Let other validators handle null checks
        }

        try {
            UUID flowId = flowIdProvider.getFlowId();
            // 1. Load the session to get the context (especially the channel)
            SessionEntity session = loginFlowService.requireAtLeast(flowId, LoginStage.IDENTIFIER_OK);

            request.setNewPassword(passwordManager.decryptPassword(request.getNewPassword(), session.getSessionId()));
            request.setNewPasswordConfirmation(passwordManager.decryptPassword(request.getNewPasswordConfirmation(), session.getSessionId()));

            if (!request.getNewPassword().equals(request.getNewPasswordConfirmation())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Password confirmation does not match")
                        .addPropertyNode("newPasswordConfirmation") // Attach the error to the 'newPasswordConfirmation' field
                        .addConstraintViolation();
                return false;
            }

            //old password decryption moved to PasswordPolicyService
            //request.setOldPassword(passwordManager.decryptPassword(request.getOldPassword(), session.getSessionId()));
            // 2. Delegate to the existing service
            boolean ok = passwordVerifier.verify(session, request.getOldPassword());

            if (!ok) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Invalid current password")
                        .addPropertyNode("oldPassword") // Attach the error to the 'oldPassword' field
                        .addConstraintViolation();
                return false;
            }
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
