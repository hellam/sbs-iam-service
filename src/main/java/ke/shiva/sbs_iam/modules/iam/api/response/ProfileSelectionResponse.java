package ke.shiva.sbs_iam.modules.iam.api.response;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProfileSelectionResponse {
    private List<ProfileSummary> profiles;
}

