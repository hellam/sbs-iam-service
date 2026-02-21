package ke.shiva.sbs_iam.modules.reference.api.mapper;

import ke.shiva.sbs_iam.modules.reference.api.request.CountryRequest;
import ke.shiva.sbs_iam.modules.reference.api.response.CountryResponse;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;

public class CountryMapper {

    public static CountryResponse toResponse(CountryEntity entity) {
        if (entity == null) {
            return null;
        }

        return CountryResponse.builder()
                .id(entity.getId())
                .countryCode(entity.getCountryCode())
                .phoneCode(entity.getPhoneCode())
                .countryName(entity.getCountryName())
                .currencyCode(entity.getCurrencyCode())
                .currencyName(entity.getCurrencyName())
                .build();
    }

    public static CountryEntity toEntity(CountryRequest request) {
        CountryEntity entity = new CountryEntity();
        entity.setCountryCode(request.getCountryCode());
        entity.setPhoneCode(request.getPhoneCode());
        entity.setCountryName(request.getCountryName());
        entity.setCurrencyCode(request.getCurrencyCode());
        entity.setCurrencyName(request.getCurrencyName());
        return entity;
    }
}
