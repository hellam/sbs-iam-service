package ke.shiva.sbs_iam.modules.iam.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.api.response.AccountDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.ClientDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Data;

@Data
public class IamRegistrationDetailsRequest {
    @NotNull(message = "Client details cannot be null")
    private ClientDetailsResponse clientDetails;

    @NotNull(message = "Account details cannot be null")
    private AccountDetailsResponse accountDetails;

    @JsonIgnore
    private Channel channel;
}
