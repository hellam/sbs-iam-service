package ke.shiva.sbs_iam.modules.iam.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Data;

@Data
public class ForgotPasswordIdentifierRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier;

    @JsonIgnore
    private Channel channel;
}
