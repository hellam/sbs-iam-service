package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeEmployeeDetailResponse {
    private Long iamUserId;
    private String clientId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String nationalId;
    private LocalDate dateOfBirth;
    private String gender;
    private String city;
    private String address;
    private String countryCode;
    private String mobile;
    private String email;
    private String status;
    private Boolean accessLocked;
    private Boolean mfaTotpEnabled;
    private String staffNo;
    private String jobTitle;
    private String department;
    private String employmentStatus;
    private Long branchId;
    private String branchName;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
