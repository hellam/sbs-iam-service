package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficePasswordPolicyUpdateRequest {
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
