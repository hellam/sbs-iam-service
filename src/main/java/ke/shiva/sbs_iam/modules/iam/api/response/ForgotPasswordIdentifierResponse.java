package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordIdentifierResponse {
    private UUID flowId;
    private String publicKey;
    private List<SecurityQuestionsResponse.SecurityQuestionDto> questions;
    private boolean securityQuestionsRequired;
    private int securityQuestionsCount;
    private boolean mfaRequired;
    private String nextStep;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityQuestionDto {
        private String id;
        private String question;
    }
}
