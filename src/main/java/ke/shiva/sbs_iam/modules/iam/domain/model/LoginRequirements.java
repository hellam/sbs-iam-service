package ke.shiva.sbs_iam.modules.iam.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequirements {

    private boolean otpRequired;
    private boolean totpRequired;
    private boolean passwordExpired;
    private boolean firstLogin;
    private boolean questionsRequired;
    private boolean profileSelectionRequired; // IB only

    public boolean isPasswordChangeRequired() {
        return passwordExpired || firstLogin;
    }

    public boolean hasPostLoginSteps() {
        return isPasswordChangeRequired() || questionsRequired || profileSelectionRequired;
    }

    public boolean nextIsProfileSelection(){
        return profileSelectionRequired && !isPasswordChangeRequired() && !questionsRequired;
    }
}


