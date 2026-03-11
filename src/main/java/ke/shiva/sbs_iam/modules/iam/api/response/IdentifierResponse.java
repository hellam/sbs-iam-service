package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

import java.util.UUID;
import java.util.List;

@Data
public class IdentifierResponse {
    private UUID flowId;

    private boolean passwordRequired;
    private boolean otpRequired;
    private boolean totpRequired;
    private short otpLength;
    private boolean passwordExpired;
    private boolean firstLogin;
    private boolean securityQuestionsRequired;

    private String publicKey; // For encrypting password on client side

    // IB-specific
    private boolean profileSelectionRequired;
    private List<String> allowedNotificationChannels;
}
