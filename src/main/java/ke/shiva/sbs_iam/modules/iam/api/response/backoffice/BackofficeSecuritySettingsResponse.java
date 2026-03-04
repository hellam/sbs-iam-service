package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BackofficeSecuritySettingsResponse {
    private Channel channel;
    private BackofficePasswordPolicyResponse passwordPolicy;
    private BackofficeMfaPolicyDetailsResponse mfaPolicy;
    private BackofficeSecurityQuestionPolicyDetailsResponse securityQuestionPolicy;
}
