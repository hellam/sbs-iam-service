package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityQuestionsResponse {
    private List<SecurityQuestionDto> questions;
    private int min;
    private int max;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityQuestionDto {
        private String id;
        private String question;
    }
}
