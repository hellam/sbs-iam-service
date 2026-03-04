package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BackofficeSecurityQuestionPolicyDetailsResponse {
    private Channel channel;
    private Boolean enabled;
    private Short minQuestions;
    private Short maxQuestions;
    private Boolean mandatory;
    private Boolean askOnForgotPassword;
    private Boolean askOnSensitiveAction;
    private Boolean isActive;
    private Short maxVerifyAttempts;
}
