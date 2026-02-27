package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeAccountRequest {
    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String iban;
    private String branchId;
    private String branchName;
    @JsonAlias("mobile")
    private String phone;
    private String email;
    private String productId;
    private String productName;
    private Boolean allowCredit;
    private Boolean allowDebit;
    private Boolean allowWaafi;
    private Boolean primary;
}
