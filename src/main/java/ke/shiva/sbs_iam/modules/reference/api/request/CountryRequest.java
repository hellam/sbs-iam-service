package ke.shiva.sbs_iam.modules.reference.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryRequest {

    @NotBlank(message = "Country code is required")
    @Size(max = 3, message = "Country code must not exceed 3 characters")
    private String countryCode;

    @NotBlank(message = "Phone code is required")
    @Size(max = 5, message = "Phone code must not exceed 5 characters")
    private String phoneCode;

    @NotBlank(message = "Country name is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    private String countryName;

    @NotBlank(message = "Currency code is required")
    @Size(max = 3, message = "Currency code must not exceed 3 characters")
    private String currencyCode;

    @NotBlank(message = "Currency name is required")
    @Size(max = 100, message = "Currency name must not exceed 100 characters")
    private String currencyName;
}
