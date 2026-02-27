package ke.shiva.sbs_iam.modules.reference.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ke.shiva.sbs_iam.modules.reference.domain.enums.BranchTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchResponse {

    private Long id;
    private String branchCode;
    private String branchName;
    private BranchTypeEnum branchType;
    private String countryCode;
    private String address;
    private String city;
    private Double longitude;
    private Double latitude;
    private Long parentBranchId;
    private String parentBranchName;
}
