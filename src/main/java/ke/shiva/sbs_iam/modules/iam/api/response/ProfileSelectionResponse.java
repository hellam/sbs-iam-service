package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProfileSelectionResponse {
    private UUID flowId;
    private List<ProfileSummary> profiles;
}

