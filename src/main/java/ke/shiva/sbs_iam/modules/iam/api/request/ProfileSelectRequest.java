package ke.shiva.sbs_iam.modules.iam.api.request;

import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import lombok.Data;

@Data
public class ProfileSelectRequest {

    @NotNull
    private ProfileType profileType;

    @NotNull
    private Long profileId;
}
