package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerDetailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.IamAuditLogEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamAuditLogRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackofficeCustomersService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserContactRepository userContactRepository;
    private final IamUserRepository iamUserRepository;
    private final IamAuditLogRepository iamAuditLogRepository;

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

    @Transactional(readOnly = true)
    public BackofficeCustomerDetailResponse getCustomer(String clientId) {
        return toDetailResponse(getRequiredCustomerProfile(clientId));
    }

    @Transactional(readOnly = true)
    public List<BackofficeAuditTrailResponse> getCustomerAuditTrail(String clientId) {
        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            return List.of();
        }

        return iamAuditLogRepository.findTop100ByIamUser_IdOrderByCreatedAtDesc(iamUser.getId()).stream()
                .map(this::toAuditTrailResponse)
                .toList();
    }

    @Transactional
    public BackofficeCustomerDetailResponse updateCustomerStatus(String clientId, IamStatus status) {
        if (status == null) {
            throw BaseException.badRequest("status is required.");
        }

        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null) {
            throw BaseException.notFound("IAM user not found for customer.");
        }

        iamUser.setStatus(status);
        iamUser.setUpdatedAt(OffsetDateTime.now());
        iamUserRepository.save(iamUser);

        return toDetailResponse(profile);
    }

    private BackofficeCustomerSummaryResponse toResponse(CustomerProfileEntity profile) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = resolvePrimaryContact(iamUser, ContactType.PHONE);
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);

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

    private BackofficeCustomerDetailResponse toDetailResponse(CustomerProfileEntity profile) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        return BackofficeCustomerDetailResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getPublicId() : null)
                .clientId(profile.getCoreCustomerId())
                .fullName(person != null ? person.getFullName() : null)
                .firstName(person != null ? person.getFirstName() : null)
                .lastName(person != null ? person.getLastName() : null)
                .nationalId(person != null ? person.getNationalId() : null)
                .dateOfBirth(person != null ? person.getDob() : null)
                .gender(person != null ? person.getGender() : null)
                .city(person != null ? person.getCity() : null)
                .address(person != null ? person.getAddress() : null)
                .countryCode(person != null && person.getCountryCode() != null ? person.getCountryCode().getCountryCode() : null)
                .mobile(resolvePrimaryContact(iamUser, ContactType.PHONE))
                .email(resolvePrimaryContact(iamUser, ContactType.EMAIL))
                .status(iamUser != null && iamUser.getStatus() != null ? iamUser.getStatus().name() : null)
                .verified(profile.getIsVerified())
                .segment(profile.getSegment())
                .language(profile.getLanguage())
                .timezone(profile.getTimezone())
                .theme(profile.getTheme())
                .allowEmail(profile.getAllowEmail())
                .allowSms(profile.getAllowSms())
                .allowPush(profile.getAllowPush())
                .lastLoginAt(iamUser != null ? iamUser.getLastLoginAt() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private BackofficeAuditTrailResponse toAuditTrailResponse(IamAuditLogEntity entry) {
        return BackofficeAuditTrailResponse.builder()
                .id(entry.getId())
                .eventType(entry.getEventType())
                .userCategory(entry.getUserCategory())
                .channel(entry.getChannel())
                .ipAddress(entry.getIpAddress())
                .deviceId(entry.getDeviceId())
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .createdAt(entry.getCreatedAt())
                .metadata(entry.getMetadata())
                .build();
    }

    private CustomerProfileEntity getRequiredCustomerProfile(String clientId) {
        String normalizedClientId = normalizeClientId(clientId);

        CustomerProfileEntity profile = customerProfileRepository.findByCoreCustomerId(normalizedClientId)
                .orElseThrow(() -> BaseException.notFound("Customer " + normalizedClientId + " not found."));

        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        if (party == null || party.getPartyType() != PartyType.PERSON) {
            throw BaseException.notFound("Customer " + normalizedClientId + " is not an individual profile.");
        }
        return profile;
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

    private String normalizeClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw BaseException.badRequest("Client ID is required.");
        }
        return clientId.trim();
    }

    private String resolvePrimaryContact(IamUserEntity iamUser, ContactType type) {
        if (iamUser == null || type == null) {
            return null;
        }
        return userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, type)
                .map(UserContact::getContactValue)
                .orElse(null);
    }
}
