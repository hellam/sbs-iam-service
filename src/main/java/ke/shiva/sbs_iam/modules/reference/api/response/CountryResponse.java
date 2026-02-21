package ke.shiva.sbs_iam.modules.reference.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryResponse {

    private Long id;
    private String countryCode;
    private String phoneCode;
    private String countryName;
    private String currencyCode;
    private String currencyName;
}
