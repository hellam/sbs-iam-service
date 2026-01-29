package ke.shiva.sbs_iam.modules.iam.domain.enums;

public enum LoginStage {
    // Login flow stages
    IDENTIFIER_OK,
    PASSWORD_OK,
    MFA_OK,
    PROFILE_PENDING,
    ACTIVE,

    // Forgot password flow stages
    FP_IDENTIFIER_OK,
    FP_SECURITY_QUESTIONS_OK,
    FP_MFA_OK,
    FP_PASSWORD_RESET
}
