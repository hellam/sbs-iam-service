package ke.shiva.sbs_iam.modules.iam.api.response;

import ke.shiva.sbs_iam.modules.iam.domain.enums.OtpType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MfaPolicyResponse {
    private Channel channel;
    private List<String> allowedNotificationChannels;
    private Boolean allowTotp;
    private Short maxVerifyAttempts;
    private OtpType otpType;
    private Short otpLength;
    private Integer otpExpirySeconds;
    private Boolean enforceOnNewDevice;
    private Boolean enforceOnNewLocation;
}
