package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PartyRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BackofficeOrganizationsService {

    private final OrganizationRepository organizationRepository;
    private final PartyRepository partyRepository;

    public PaginatedResponse<BackofficeOrganizationSummaryResponse> getOrganizations(HttpServletRequest request) {
        validateFilters(request);

        List<String> searchableColumns = List.of(
                "party.coreCustomerId",
                "displayName",
                "legalName",
                "registrationNo",
                "companyPhone",
                "companyEmail",
                "city",
                "customerSegment",
                "contactPersonName"
        );
        List<String> sortableColumns = List.of(
                "createdAt",
                "updatedAt",
                "displayName",
                "legalName",
                "customerSegment",
                "smeMode",
                "party.coreCustomerId",
                "party.status",
                "countryCode.countryName"
        );
        List<String> filterableColumns = List.of(
                "customerSegment",
                "smeMode",
                "party.status",
                "countryCode.countryCode"
        );

        Page<OrganizationEntity> page = PaginationUtil.filterAndPaginateWithScoping(
                organizationRepository,
                request,
                searchableColumns,
                sortableColumns,
                filterableColumns,
                "party.partyType",
                PartyType.ORGANIZATION.name(),
                10
        );

        Page<BackofficeOrganizationSummaryResponse> dtoPage = page.map(this::toResponse);
        return PaginationUtil.toPaginatedResponse(dtoPage);
    }

    private BackofficeOrganizationSummaryResponse toResponse(OrganizationEntity organization) {
        PartyEntity party = partyRepository.findById(organization.getId()).orElse(null);
        CountryEntity country = organization.getCountryCode();

        String displayName = StringUtils.hasText(organization.getDisplayName())
                ? organization.getDisplayName()
                : organization.getLegalName();

        return BackofficeOrganizationSummaryResponse.builder()
                .partyId(organization.getId())
                .clientId(party != null ? party.getCoreCustomerId() : null)
                .displayName(displayName)
                .legalName(organization.getLegalName())
                .registrationNo(organization.getRegistrationNo())
                .customerSegment(organization.getCustomerSegment())
                .smeMode(organization.getSmeMode())
                .companyPhone(organization.getCompanyPhone())
                .companyEmail(organization.getCompanyEmail())
                .city(organization.getCity())
                .country(country != null ? country.getCountryName() : null)
                .status(party != null ? party.getStatus() : null)
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    private void validateFilters(HttpServletRequest request) {
        String smeMode = firstNonBlank(request.getParameter("smeMode"));
        if (StringUtils.hasText(smeMode)
                && !"true".equalsIgnoreCase(smeMode.trim())
                && !"false".equalsIgnoreCase(smeMode.trim())) {
            throw BaseException.badRequest("smeMode must be either true or false.");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
