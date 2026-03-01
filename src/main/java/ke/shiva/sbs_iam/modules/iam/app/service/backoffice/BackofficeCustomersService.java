package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
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
public class BackofficeCustomersService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserContactRepository userContactRepository;

    public PaginatedResponse<BackofficeCustomerSummaryResponse> getCustomers(HttpServletRequest request) {
        validateFilters(request);

        List<String> searchableColumns = List.of(
                "coreCustomerId",
                "iamUser.party.person.fullName",
                "iamUser.contacts.contactValue"
        );
        List<String> sortableColumns = List.of(
                "createdAt",
                "updatedAt",
                "coreCustomerId",
                "isVerified",
                "iamUser.status",
                "iamUser.party.person.fullName"
        );
        List<String> filterableColumns = List.of(
                "isVerified",
                "iamUser.status"
        );

        Page<CustomerProfileEntity> page = PaginationUtil.filterAndPaginateWithScoping(
                customerProfileRepository,
                request,
                searchableColumns,
                sortableColumns,
                filterableColumns,
                "iamUser.party.partyType",
                PartyType.PERSON.name(),
                10
        );
        Page<BackofficeCustomerSummaryResponse> dtoPage = page.map(this::toResponse);
        return PaginationUtil.toPaginatedResponse(dtoPage);
    }

    private BackofficeCustomerSummaryResponse toResponse(CustomerProfileEntity profile) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = iamUser != null
                ? userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, ContactType.PHONE)
                .map(UserContact::getContactValue)
                .orElse(null)
                : null;

        String email = iamUser != null
                ? userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, ContactType.EMAIL)
                .map(UserContact::getContactValue)
                .orElse(null)
                : null;

        return BackofficeCustomerSummaryResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getPublicId() : null)
                .clientId(profile.getCoreCustomerId())
                .fullName(person != null ? person.getFullName() : null)
                .mobile(mobile)
                .email(email)
                .status(iamUser != null && iamUser.getStatus() != null ? iamUser.getStatus().name() : null)
                .verified(profile.getIsVerified())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    private void validateFilters(HttpServletRequest request) {
        String status = firstNonBlank(
                request.getParameter("status"),
                request.getParameter("iamUser.status"),
                request.getParameter("iamUser_status")
        );

        if (StringUtils.hasText(status)) {
            try {
                IamStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw BaseException.badRequest("Invalid status. Allowed values: ACTIVE, INACTIVE, LOCKED, SUSPENDED.");
            }
        }

        String isVerified = firstNonBlank(request.getParameter("isVerified"));
        if (StringUtils.hasText(isVerified)
                && !"true".equalsIgnoreCase(isVerified.trim())
                && !"false".equalsIgnoreCase(isVerified.trim())) {
            throw BaseException.badRequest("isVerified must be either true or false.");
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
