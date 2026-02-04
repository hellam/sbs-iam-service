package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ForgotPasswordSecurityQuestionsRequest {
    @NotNull(message = "Answers are required")
    private List<SecurityQuestionAnswer> answers;

    @Data
    public static class SecurityQuestionAnswer {
        @NotNull(message = "Question ID is required")
        private String questionId;

        @NotNull(message = "Answer is required")
        private String answer;
    }
}
