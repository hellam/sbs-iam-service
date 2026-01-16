package ke.shiva.sbs_iam.modules.iam.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Data;

@Data
public class IdentifierRequest {
    @NotBlank
    @Size(min = 3, max =20, message = "Invalid identifier")
    private String identifier;

    @JsonIgnore
    private Channel channel;
}
