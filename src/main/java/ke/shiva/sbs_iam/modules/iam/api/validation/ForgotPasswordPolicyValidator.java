package ke.shiva.sbs_iam.modules.iam.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ke.shiva.sbs_iam.modules.iam.api.request.ForgotPasswordResetRequest;
import ke.shiva.sbs_iam.modules.iam.app.service.ForgotPasswordFlowService;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordManager;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordPolicyService;
import ke.shiva.sbs_iam.modules.iam.app.util.FlowIdProvider;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.util.TransitPasswordCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForgotPasswordPolicyValidator implements ConstraintValidator<ValidForgotPasswordPolicy, ForgotPasswordResetRequest> {

    private final PasswordPolicyService passwordPolicyService;
    private final ForgotPasswordFlowService forgotPasswordFlowService;
    private final FlowIdProvider flowIdProvider;
    private final PasswordManager passwordManager;

    @Override
    public boolean isValid(ForgotPasswordResetRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getNewPassword() == null) {
            return true; // Let other validators handle null checks
        }

        try {
            UUID flowId = flowIdProvider.getFlowId();

            // Load the forgot password session to get the context (channel, user)
            SessionEntity session = forgotPasswordFlowService.requireAtLeast(flowId, LoginStage.FP_IDENTIFIER_OK);

            // Decrypt the new password for validation
//            request.setNewPassword(passwordManager.decryptPassword(request.getNewPassword(),session.getSessionId()));

            // Get password policy for the channel
            PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(session.getChannel());

            // Validate password structure (length, complexity, etc.)
            passwordPolicyService.validateStructure(request.getNewPassword(), policy);

            // Validate against password history
            passwordPolicyService.validateAgainstHistory(session.getIamUser(), request.getNewPassword(), policy);

            // Validate against common passwords
            passwordPolicyService.validateCommonPasswords(request.getNewPassword(), policy);

        } catch (Exception e) {
            // If validation fails, the service throws an exception
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(e.getMessage())
                   .addPropertyNode("newPassword") // Attach the error to the 'newPassword' field
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}
