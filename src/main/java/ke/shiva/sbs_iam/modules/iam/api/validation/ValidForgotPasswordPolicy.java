package ke.shiva.sbs_iam.modules.iam.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ForgotPasswordPolicyValidator.class)
@Target({ElementType.TYPE}) // Applies to the Class level
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidForgotPasswordPolicy {
    String message() default "Invalid password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
