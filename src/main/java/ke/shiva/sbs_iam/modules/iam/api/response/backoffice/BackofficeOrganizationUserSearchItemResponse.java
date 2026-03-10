package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeOrganizationUserSearchItemResponse {
    private Long iamUserId;
    private String individualClientId;
    private String fullName;
    private String mobile;
    private String email;
    private Boolean verified;
    private Boolean internetLocked;
    private Boolean mfaTotpEnabled;
    private Boolean alreadyLinked;
}
