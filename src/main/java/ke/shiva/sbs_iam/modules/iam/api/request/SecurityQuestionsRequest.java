package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SecurityQuestionsRequest {

    @NotNull
    private UUID flowId;

    @NotEmpty
    private List<QuestionAnswer> questions;

    @Data
    public static class QuestionAnswer {
        @NotNull
        private Long questionId;

        @NotBlank
        private String answer;
    }
}
