package ke.shiva.sbs_iam.modules.iam.api.response;

import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionPolicyResponse {
    private Channel channel;
    private Integer inactivityTimeoutSeconds;
    private Integer warningCountdownSeconds;
}
