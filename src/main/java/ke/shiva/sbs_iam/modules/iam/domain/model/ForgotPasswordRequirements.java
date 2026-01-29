package ke.shiva.sbs_iam.modules.iam.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordRequirements {
    private boolean securityQuestionsRequired;
    private int securityQuestionsCount;
    private boolean mfaRequired;
}
