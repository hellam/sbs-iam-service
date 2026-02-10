package ke.shiva.sbs_iam.modules.iam.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Data;

@Data
public class IamRegistrationDetailsRequest {
    // ==================== CLIENT / IDENTITY ====================
    @NotNull @NotEmpty
    private String clientId;

    @NotNull @NotEmpty
    private String firstName;

    private String middleName; // Optional

    @NotNull @NotEmpty
    private String lastName;

    @NotNull @NotEmpty
    private String nationalId;

    // ==================== CONTACTS ====================
    @NotNull @NotEmpty
    private String mobile;

    @NotNull @NotEmpty
    private String email;

    @NotNull @NotEmpty
    private String city;

    @NotNull @NotEmpty
    private String country;

    @NotNull @NotEmpty
    private String address;

    // ==================== ACCOUNTS ====================
    @NotNull @NotEmpty
    private String accountNumber;

    @NotNull @NotEmpty
    private String productId;

    @NotNull @NotEmpty
    private String productName;

    @NotNull @NotEmpty
    private String currency;

    @NotNull @NotEmpty
    private String status;

    // ==================== REGISTRATION CHANNEL ====================
    @JsonIgnore
    private Channel channel;

}
