package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SecurityQuestionsRequest {

    @NotEmpty
    private List<QuestionAnswer> questions;

    @Data
    public static class QuestionAnswer {
        @NotNull
        private String questionId;

        @NotBlank
        private String answer;
    }
}
