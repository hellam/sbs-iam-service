package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeOrganizationPermissionResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private Boolean isTransaction;
    private Boolean enabled;
}
