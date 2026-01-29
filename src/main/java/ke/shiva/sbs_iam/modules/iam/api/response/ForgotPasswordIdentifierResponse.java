package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordIdentifierResponse {
    private UUID flowId;
    private boolean securityQuestionsRequired;
    private int securityQuestionsCount;
    private boolean mfaRequired;
    private String nextStep;
}
