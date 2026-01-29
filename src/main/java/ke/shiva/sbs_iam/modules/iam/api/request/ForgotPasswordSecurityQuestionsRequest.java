package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ForgotPasswordSecurityQuestionsRequest {
    @NotNull(message = "Flow ID is required")
    private UUID flowId;

    @NotNull(message = "Answers are required")
    private List<SecurityQuestionAnswer> answers;

    @Data
    public static class SecurityQuestionAnswer {
        @NotNull(message = "Question ID is required")
        private Long questionId;

        @NotNull(message = "Answer is required")
        private String answer;
    }
}
