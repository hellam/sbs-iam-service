package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeOrganizationRoleResponse {
    private Long id;
    private String name;
    private String description;
    private String taskRole;
    private Boolean isDefault;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
