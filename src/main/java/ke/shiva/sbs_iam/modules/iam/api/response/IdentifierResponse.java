package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

import java.util.UUID;

@Data
public class IdentifierResponse {
    private UUID flowId;

    private boolean passwordRequired;
    private boolean mfaRequired;
    private boolean passwordExpired;
    private boolean firstLogin;
    private boolean securityQuestionsRequired;

    // IB-specific
    private boolean profileSelectionRequired;
}

