package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

import java.util.UUID;

@Data
public class PasswordStepResponse {
    private boolean otpRequired;
    private boolean totpRequired;
    private boolean passwordChangeRequired;
    private boolean securityQuestionsRequired;
    private boolean profileSelectionRequired;
}
