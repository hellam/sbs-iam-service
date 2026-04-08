package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.client.account.dto.response.BackofficeCustomerDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerDetailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.GeneratedPasswordService;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordUpdateService;
import ke.shiva.sbs_iam.modules.iam.app.service.SessionRevocationService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.IamAuditLogEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.external.NotificationService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamAuditLogRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeCustomersService {

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final UserContactRepository userContactRepository;
    private final IamUserRepository iamUserRepository;
    private final IamAuditLogRepository iamAuditLogRepository;
    private final PasswordUpdateService passwordUpdateService;
    private final SessionRevocationService sessionRevocationService;
    private final NotificationService notificationService;
    private final GeneratedPasswordService generatedPasswordService;
    private final CountryRepository countryRepository;
    private final BackofficeOnboardingService onboardingService;

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

    @Transactional
    public BackofficeCustomerDetailResponse updateCustomerAccessLock(String clientId, boolean blocked) {
        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.notFound("IAM user not found for customer.");
        }

        CustomerAuthEntity auth = customerAuthRepository.findByIamUserId(iamUser.getId())
                .orElseThrow(() -> BaseException.notFound("Customer credentials not found."));

        auth.setInternetLocked(blocked);
        if (blocked) {
            auth.setInternetLockoutUntil(null);
        } else {
            auth.setInternetLockoutUntil(null);
            auth.setInternetFailedAttempts((short) 0);
        }
        customerAuthRepository.save(auth);

        if (blocked) {
            sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_CUSTOMER_BLOCKED");
        }

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeCustomerDetailResponse resetCustomerPassword(String clientId) {
        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.notFound("IAM user not found for customer.");
        }

        String randomPassword = generatedPasswordService.generateTemporaryPassword(Channel.INTERNET_BANKING, 16);
        passwordUpdateService.updatePassword(iamUser, randomPassword, Channel.INTERNET_BANKING, true);
        sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_CUSTOMER_PASSWORD_RESET");
        sendPasswordResetNotification(iamUser, profile.getCoreCustomerId(), randomPassword);

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeCustomerDetailResponse resetCustomerMfa(String clientId) {
        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.notFound("IAM user not found for customer.");
        }

        CustomerAuthEntity auth = customerAuthRepository.findByIamUserId(iamUser.getId())
                .orElseThrow(() -> BaseException.notFound("Customer credentials not found."));
        if (!isMfaTotpConfigured(auth)) {
            throw BaseException.badRequest("MFA TOTP is not configured for this user.");
        }
        auth.setMfaEnabled(false);
        auth.setMfaSecret(null);
        auth.setMfaLastVerifiedAt(null);
        customerAuthRepository.save(auth);

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeCustomerDetailResponse syncCustomerKyc(String clientId) {
        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;
        if (iamUser == null || party == null || person == null) {
            throw BaseException.notFound("Customer profile is incomplete.");
        }

        BackofficeCustomerDetailsResponse coreDetails = onboardingService.fetchCustomerCoreDetails(profile.getCoreCustomerId());
        String[] names = resolvePersonNames(coreDetails);

        person.setFirstName(names[0]);
        person.setLastName(names[2]);
        person.setFullName(buildFullName(names[0], names[1], names[2]));
        person.setCity(trimToNull(coreDetails.getCity()));
        person.setAddress(trimToNull(coreDetails.getAddress1()));
        person.setCountryCode(resolveCountry(coreDetails.getCountryId(), coreDetails.getCountryName()));
        person.setUpdatedAt(OffsetDateTime.now());

        upsertPrimaryContact(iamUser, ContactType.EMAIL, trimToNull(coreDetails.getEmail()));
        upsertPrimaryContact(iamUser, ContactType.PHONE, trimToNull(coreDetails.getMobile()));

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
                .accessLocked(isAccessLocked(iamUser))
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
                .accessLocked(isAccessLocked(iamUser))
                .mfaTotpEnabled(isMfaTotpEnabled(iamUser))
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

    private boolean isAccessLocked(IamUserEntity iamUser) {
        if (iamUser == null || iamUser.getId() == null) {
            return false;
        }
        return customerAuthRepository.findByIamUserId(iamUser.getId())
                .map(CustomerAuthEntity::getInternetLocked)
                .orElse(false);
    }

    private boolean isMfaTotpEnabled(IamUserEntity iamUser) {
        if (iamUser == null || iamUser.getId() == null) {
            return false;
        }
        return customerAuthRepository.findByIamUserId(iamUser.getId())
                .map(this::isMfaTotpConfigured)
                .orElse(false);
    }

    private boolean isMfaTotpConfigured(CustomerAuthEntity auth) {
        return auth != null
                && Boolean.TRUE.equals(auth.getMfaEnabled())
                && StringUtils.hasText(auth.getMfaSecret());
    }

    private void upsertPrimaryContact(IamUserEntity iamUser, ContactType type, String value) {
        if (iamUser == null || iamUser.getId() == null || type == null || !StringUtils.hasText(value)) {
            return;
        }

        String normalized = value.trim();
        if (type == ContactType.EMAIL) {
            if (userContactRepository.existsByContactTypeAndContactValueAndIamUser_IdNot(type, normalized, iamUser.getId())) {
                throw BaseException.badRequest("Email '" + normalized + "' is already registered.");
            }
        } else if (type == ContactType.PHONE) {
            String partial = normalized.length() > 9 ? normalized.substring(normalized.length() - 9) : normalized;
            if (userContactRepository.existsByContactTypeAndContactValueContainingAndIamUser_IdNot(type, partial, iamUser.getId())) {
                throw BaseException.badRequest("Phone number '" + normalized + "' is already registered.");
            }
        }

        UserContact contact = userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, type)
                .orElseGet(() -> {
                    UserContact created = new UserContact();
                    created.setIamUser(iamUser);
                    created.setContactType(type);
                    created.setPrimary(true);
                    created.setCreatedAt(OffsetDateTime.now());
                    return created;
                });

        contact.setContactValue(normalized);
        contact.setUpdatedAt(OffsetDateTime.now());
        userContactRepository.save(contact);
    }

    private void sendPasswordResetNotification(IamUserEntity iamUser, String clientId, String temporaryPassword) {
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);
        if (!StringUtils.hasText(email)) {
            return;
        }

        String fullName = iamUser.getParty() != null && iamUser.getParty().getPerson() != null
                ? iamUser.getParty().getPerson().getFullName()
                : "Customer";

        try {
            notificationService.sendAdminPasswordResetNotice(email, fullName, clientId, temporaryPassword);
        } catch (Exception exception) {
            log.warn("Password reset notification failed for customer {}: {}", clientId, exception.getMessage());
        }
    }

    private CountryEntity resolveCountry(String countryCode, String countryName) {
        String byCode = trimToNull(countryCode);
        if (byCode != null) {
            return countryRepository.findByCountryCode(byCode.toUpperCase()).orElse(null);
        }

        String byName = trimToNull(countryName);
        if (byName != null) {
            return countryRepository.findByCountryNameIgnoreCase(byName).orElse(null);
        }
        return null;
    }

    private String[] resolvePersonNames(BackofficeCustomerDetailsResponse response) {
        String first = trimToNull(response.getFirstName());
        String middle = trimToNull(response.getMiddleName());
        String last = trimToNull(response.getLastName());
        if (first != null && last != null) {
            return new String[]{first, middle, last};
        }

        String fullName = trimToNull(response.getFullName());
        if (fullName == null) {
            throw BaseException.badRequest("Full name is required from core banking.");
        }

        String[] parts = fullName.split("\\s+");
        if (parts.length == 0) {
            throw BaseException.badRequest("Full name is required from core banking.");
        }

        first = parts[0];
        if (parts.length == 1) {
            last = parts[0];
            middle = null;
        } else {
            last = parts[parts.length - 1];
            middle = parts.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)) : null;
        }

        return new String[]{first, middle, last};
    }

    private String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(firstName)) {
            builder.append(firstName.trim());
        }
        if (StringUtils.hasText(middleName)) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(middleName.trim());
        }
        if (StringUtils.hasText(lastName)) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(lastName.trim());
        }
        return builder.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
