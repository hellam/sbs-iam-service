package ke.shiva.sbs_iam.modules.iam.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.ChannelEnum;
import lombok.Data;

@Data
public class IdentifierRequest {
    @NotBlank
    private String identifier;

    @JsonIgnore
    private ChannelEnum channel;
}
