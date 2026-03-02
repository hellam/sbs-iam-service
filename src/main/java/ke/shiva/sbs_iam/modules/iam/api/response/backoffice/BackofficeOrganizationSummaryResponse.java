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
public class BackofficeOrganizationSummaryResponse {
    private Long partyId;
    private String clientId;
    private String displayName;
    private String legalName;
    private String registrationNo;
    private String customerSegment;
    private Boolean smeMode;
    private String companyPhone;
    private String companyEmail;
    private String city;
    private String country;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
