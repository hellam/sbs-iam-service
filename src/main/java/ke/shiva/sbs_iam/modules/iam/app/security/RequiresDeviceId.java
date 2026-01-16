package ke.shiva.sbs_iam.modules.iam.app.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce device ID validation on controller methods.
 * Similar to Laravel middleware, this provides declarative security for device-based authentication.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresDeviceId {

    /**
     * The validation mode to apply.
     * - EXISTENCE_ONLY: Validates that device ID cookie exists and is registered
     * - SESSION_BOUND: Validates device ID matches the current session's device
     */
    DeviceValidationMode mode() default DeviceValidationMode.EXISTENCE_ONLY;

    /**
     * Whether the device ID is required or optional.
     * When false, validation only occurs if device ID is present.
     */
    boolean required() default true;
}

