package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeOrganizationUserSearchResponse {
    private List<BackofficeOrganizationUserSearchItemResponse> candidates;
    private Boolean coreIndividualFound;
    private String coreIndividualClientId;
}
