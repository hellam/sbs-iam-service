package ke.shiva.sbs_iam.modules.iam.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequirements {

    private boolean mfaRequired;
    private boolean passwordExpired;
    private boolean firstLogin;
    private boolean questionsRequired;
    private boolean profileSelectionRequired;

    public boolean passwordChangeRequired() {
        return firstLogin || passwordExpired;
    }
}

