package ke.shiva.sbs_iam.modules.iam.app.security;

/**
 * Defines the validation mode for device ID verification in the authentication flow.
 */
public enum DeviceValidationMode {
    /**
     * Only checks that a device ID exists (cookie present and device exists in database).
     * Used for identifier lookup and initial authentication steps.
     */
    EXISTENCE_ONLY,

    /**
     * Validates that the device ID exists AND matches the device ID in the active session.
     * Used for password authentication, token refresh, and other operations requiring session binding.
     */
    SESSION_BOUND
}
