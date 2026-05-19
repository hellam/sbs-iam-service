package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeCustomerDetailResponse {
    private UUID iamUserId;
    private String clientId;
    private String username;
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
    private String internetAccessStatus;
    private Boolean internetAccessActive;
    private Boolean internetPasswordSet;
    private Boolean internetLocked;
    private String mobileAccessStatus;
    private Boolean mobileAccessActive;
    private Boolean mobilePinSet;
    private Boolean mobileLocked;
    private Boolean mfaTotpEnabled;
    private Boolean verified;
    private String segment;
    private String language;
    private String timezone;
    private String theme;
    private Boolean allowEmail;
    private Boolean allowSms;
    private Boolean allowPush;
    private OffsetDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
