package ke.shiva.sbs_iam.modules.iam.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordPolicyValidator.class)
@Target({ElementType.TYPE}) // Applies to the Class level
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPasswordPolicy {
    String message() default "Invalid password";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
