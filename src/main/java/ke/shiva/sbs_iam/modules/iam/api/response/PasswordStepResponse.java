package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

import java.util.List;

@Data
public class PasswordStepResponse {
    private boolean otpRequired;
    private boolean totpRequired;
    private short otpLength;
    private boolean passwordChangeRequired;
    private boolean securityQuestionsRequired;
    private boolean profileSelectionRequired;
    private String phoneNumber; // for OTP
    private String email; // for OTP
    private List<String> allowedNotificationChannels;
}
