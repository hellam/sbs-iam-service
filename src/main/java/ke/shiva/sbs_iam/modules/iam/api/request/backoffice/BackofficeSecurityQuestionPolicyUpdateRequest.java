package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeSecurityQuestionPolicyUpdateRequest {
    private Boolean enabled;
    private Short minQuestions;
    private Short maxQuestions;
    private Boolean mandatory;
    private Boolean askOnForgotPassword;
    private Boolean askOnSensitiveAction;
    private Boolean isActive;
    private Short maxVerifyAttempts;
}
