package ke.shiva.sbs_iam.modules.reference.api.mapper;

import ke.shiva.sbs_iam.modules.reference.api.request.BranchRequest;
import ke.shiva.sbs_iam.modules.reference.api.response.BranchResponse;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;

public class BranchMapper {

    public static BranchResponse toInternalResponse(BranchEntity entity) {
        if (entity == null) {
            return null;
        }

        return BranchResponse.builder()
                .id(entity.getId())
                .branchCode(entity.getBranchCode())
                .branchName(entity.getBranchName())
                .branchType(entity.getBranchTypeEnum())
                .countryCode(entity.getCountryCode() != null ? entity.getCountryCode().getCountryCode() : null)
                .address(entity.getAddress())
                .city(entity.getCity())
                .longitude(entity.getLongitude())
                .latitude(entity.getLatitude())
                .parentBranchId(entity.getParentBranch() != null ? entity.getParentBranch().getId() : null)
                .parentBranchName(entity.getParentBranch() != null ? entity.getParentBranch().getBranchName() : null)
                .build();
    }


    public static BranchResponse toPublicResponse(BranchEntity entity) {
        if (entity == null) {
            return null;
        }

        return BranchResponse.builder()
                .branchCode(entity.getBranchCode())
                .branchName(entity.getBranchName())
                .build();
    }

    public static BranchEntity toEntity(BranchRequest request, CountryEntity countryEntity, BranchEntity parentBranch) {
        BranchEntity entity = new BranchEntity();
        entity.setBranchCode(request.getBranchCode());
        entity.setBranchName(request.getBranchName());
        entity.setBranchTypeEnum(request.getBranchType());
        entity.setCountryCode(countryEntity);
        entity.setAddress(request.getAddress());
        entity.setCity(request.getCity());
        entity.setLongitude(request.getLongitude());
        entity.setLatitude(request.getLatitude());
        entity.setParentBranch(parentBranch);
        return entity;
    }
}
