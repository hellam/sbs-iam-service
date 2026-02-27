package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeCustomerLookupResponse {
    private String clientId;
    private String fullName;
    private String mobile;
    private String email;
    private String country;
    private String city;
    private String openedDate;
    private String address;
}
