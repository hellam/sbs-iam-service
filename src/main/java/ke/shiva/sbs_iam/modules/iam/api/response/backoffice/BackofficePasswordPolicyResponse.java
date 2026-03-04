package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BackofficePasswordPolicyResponse {
    private Channel channel;
    private Short minLength;
    private Short maxLength;
    private Boolean requireUppercase;
    private Boolean requireLowercase;
    private Boolean requireNumber;
    private Boolean requireSymbol;
    private Boolean blockCommonPasswords;
    private Short passwordHistoryCount;
    private Boolean expirationEnabled;
    private Short expirationDays;
    private Short maxFailedAttempts;
    private Short lockoutMinutes;
    private Boolean requireFactoryReset;
    private String hashAlgorithm;
    private Short hashCost;
}
