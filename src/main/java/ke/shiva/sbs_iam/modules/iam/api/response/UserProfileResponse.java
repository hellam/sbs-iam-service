package ke.shiva.sbs_iam.modules.iam.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {
    private String displayName;
    private String organization;
    private ProfileType profileType;
    private String identifier;
    private boolean hasMultipleProfiles;
}
