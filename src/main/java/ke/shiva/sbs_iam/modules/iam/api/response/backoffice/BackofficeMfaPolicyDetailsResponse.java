package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import ke.shiva.sbs_iam.modules.iam.domain.enums.OtpType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.TransactionMfaMode;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BackofficeMfaPolicyDetailsResponse {
    private Channel channel;
    private List<String> availableNotificationChannels;
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
