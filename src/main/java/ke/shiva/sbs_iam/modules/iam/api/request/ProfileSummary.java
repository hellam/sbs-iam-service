package ke.shiva.sbs_iam.modules.iam.api.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileSummary {
    private String profileType;
    private Long profileId;
    private String displayName;
}
