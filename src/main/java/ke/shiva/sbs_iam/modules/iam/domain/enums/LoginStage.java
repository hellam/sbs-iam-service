package ke.shiva.sbs_iam.modules.iam.domain.enums;

//ENUM Length should not exceed 20 characters
public enum LoginStage {
    // Login flow stages
    IDENTIFIER_OK,
    PASSWORD_OK,
    TOTP_ENROLL_REQUIRED,
    MFA_OK,
    PROFILE_PENDING,
    ACTIVE,

    // Forgot password flow stages
    FP_IDENTIFIER_OK,
    FP_SEC_QNS_OK,
    FP_MFA_OK,
    FP_PASSWORD_RESET
}
