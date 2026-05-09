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
public class BackofficeEmployeeSummaryResponse {
    private Long iamUserId;
    private String clientId;
    private String username;
    private String fullName;
    private String staffNo;
    private String mobile;
    private String email;
    private String jobTitle;
    private String department;
    private String employmentStatus;
    private Long branchId;
    private String branchName;
    private String status;
    private Boolean accessLocked;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
