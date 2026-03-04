package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import ke.shiva.sbs_iam.modules.iam.domain.enums.OtpType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.TransactionMfaMode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BackofficeMfaPolicyUpdateRequest {
    private List<String> allowedNotificationChannels;
    private Boolean allowTotp;
    private Short maxVerifyAttempts;
    private OtpType otpType;
    private Short otpLength;
    private TransactionMfaMode transactionMfaMode;
    private Boolean enforceOnTransactionInitiation;
    private Boolean enforceOnTransactionApproval;
    private Boolean enforceOnTransactionRejection;
    private Integer otpExpirySeconds;
    private Short otpDailyLimit;
    private Boolean enforceOnNewDevice;
    private Boolean enforceOnNewLocation;
}
