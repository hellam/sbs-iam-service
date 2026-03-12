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
public class BackofficeOrganizationUserResponse {
    private Long organizationUserId;
    private Long iamUserId;
    private String individualClientId;
    private String clientId;
    private String fullName;
    private String mobile;
    private String email;
    private Boolean verified;
    private Boolean internetLocked;
    private Boolean mfaTotpEnabled;
    private Long orgRoleId;
    private String roleName;
    private String taskRole;
    private Boolean primary;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
