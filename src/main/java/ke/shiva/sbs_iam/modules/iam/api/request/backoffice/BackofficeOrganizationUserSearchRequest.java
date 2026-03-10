package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeOrganizationUserSearchRequest {
    private String customerId;
    private String phone;
    private String idNumber;
    private String email;
    private String passport;
}
