package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeCustomerSummaryResponse {
    private UUID iamUserId;
    private String clientId;
    private String username;
    private String fullName;
    private String mobile;
    private String email;
    private String status;
    private Boolean accessLocked;
    private Boolean verified;
    private LocalDateTime createdAt;
}
