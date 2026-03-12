package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeOrganizationRoleDetailsResponse {
    private Long id;
    private String name;
    private String description;
    private String taskRole;
    private Boolean isDefault;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<BackofficeOrganizationPermissionResponse> permissions;
}
