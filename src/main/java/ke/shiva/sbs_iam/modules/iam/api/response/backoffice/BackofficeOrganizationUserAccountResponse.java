package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeOrganizationUserAccountResponse {
    private String clientAccountId;
    private String accountNumber;
    private String accountName;
    private String currency;
    private String productName;
    private String iban;
    private Boolean allowCredit;
    private Boolean allowDebit;
}
