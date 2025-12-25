package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileSummary {
    private String profileType;
    private Long profileId;
    private String displayName;
}
