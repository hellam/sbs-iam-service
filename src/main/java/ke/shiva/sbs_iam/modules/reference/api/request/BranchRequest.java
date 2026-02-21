package ke.shiva.sbs_iam.modules.reference.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.reference.domain.enums.BranchTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code must not exceed 20 characters")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    private String branchName;

    @NotNull(message = "Branch type is required")
    private BranchTypeEnum branchType;

    @NotBlank(message = "Country code is required")
    @Size(max = 3, message = "Country code must not exceed 3 characters")
    private String countryCode;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    private Double longitude;

    private Double latitude;

    private Long parentBranchId;
}
