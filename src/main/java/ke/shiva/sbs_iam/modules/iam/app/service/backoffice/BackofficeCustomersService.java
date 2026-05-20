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
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
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
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import ke.shiva.shivacorestarter.util.UsernameGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeCustomersService {

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final UserContactRepository userContactRepository;
    private final IamUserRepository iamUserRepository;
    private final BackofficeAuditTrailService auditTrailService;
    private final LoginIdentifierRepository loginIdentifierRepository;
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
    public PaginatedResponse<BackofficeAuditTrailResponse> getCustomerAuditTrail(
            String clientId,
            HttpServletRequest request
    ) {
        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            return auditTrailService.getUserAuditTrail(null, null, request);
        }

        return auditTrailService.getUserAuditTrail(iamUser, profile.getCoreCustomerId(), request);
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
        auditTrailService.recordUserAudit(
                iamUser,
                "CUSTOMER",
                "BACKOFFICE_CUSTOMER_STATUS_UPDATED",
                "BACKOFFICE",
                "CUSTOMER_PROFILE",
                profile.getId(),
                auditMetadata(profile.getCoreCustomerId(), "status", status.name())
        );

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeCustomerDetailResponse updateCustomerInternetAccess(String clientId, boolean enabled) {
        CustomerProfileEntity profile = getRequiredCustomerProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.notFound("IAM user not found for customer.");
        }

        if (enabled && iamUser.getStatus() != IamStatus.ACTIVE) {
            throw BaseException.badRequest("Activate the customer profile before enabling internet banking.");
        }

        LoginIdentifierEntity identifier = getOrCreateInternetIdentifier(iamUser);
        CustomerAuthEntity auth = getOrCreateCustomerAuth(iamUser);
        boolean generatedPassword = false;
        String temporaryPassword = null;

        if (enabled) {
            identifier.setStatus(IamStatus.ACTIVE);
            auth.setInternetLocked(false);
            auth.setInternetLockoutUntil(null);
            auth.setInternetFailedAttempts((short) 0);

            if (!StringUtils.hasText(auth.getInternetPasswordHash())) {
                temporaryPassword = generatedPasswordService.generateTemporaryPassword(Channel.INTERNET_BANKING, 16);
                customerAuthRepository.save(auth);
                passwordUpdateService.updatePassword(iamUser, temporaryPassword, Channel.INTERNET_BANKING, true);
                generatedPassword = true;
            }
        } else {
            identifier.setStatus(IamStatus.INACTIVE);
            sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_CUSTOMER_INTERNET_DISABLED");
        }

        identifier.setUpdatedAt(OffsetDateTime.now());
        loginIdentifierRepository.save(identifier);
        customerAuthRepository.save(auth);

        if (generatedPassword) {
            sendPasswordResetNotification(iamUser, profile.getCoreCustomerId(), temporaryPassword);
        }

        auditTrailService.recordUserAudit(
                iamUser,
                "CUSTOMER",
                enabled ? "BACKOFFICE_CUSTOMER_INTERNET_ENABLED" : "BACKOFFICE_CUSTOMER_INTERNET_DISABLED",
                "BACKOFFICE",
                "CUSTOMER_PROFILE",
                profile.getId(),
                auditMetadata(
                        profile.getCoreCustomerId(),
                        "enabled", enabled,
                        "password_generated", generatedPassword,
                        "username", identifier.getIdentifier()
                )
        );

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
        auditTrailService.recordUserAudit(
                iamUser,
                "CUSTOMER",
                blocked ? "BACKOFFICE_CUSTOMER_ACCESS_BLOCKED" : "BACKOFFICE_CUSTOMER_ACCESS_UNBLOCKED",
                "BACKOFFICE",
                "CUSTOMER_PROFILE",
                profile.getId(),
                auditMetadata(profile.getCoreCustomerId(), "blocked", blocked)
        );

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
        auditTrailService.recordUserAudit(
                iamUser,
                "CUSTOMER",
                "BACKOFFICE_CUSTOMER_PASSWORD_RESET",
                "BACKOFFICE",
                "CUSTOMER_PROFILE",
                profile.getId(),
                auditMetadata(profile.getCoreCustomerId())
        );

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
        auditTrailService.recordUserAudit(
                iamUser,
                "CUSTOMER",
                "BACKOFFICE_CUSTOMER_MFA_RESET",
                "BACKOFFICE",
                "CUSTOMER_PROFILE",
                profile.getId(),
                auditMetadata(profile.getCoreCustomerId())
        );

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
        auditTrailService.recordUserAudit(
                iamUser,
                "CUSTOMER",
                "BACKOFFICE_CUSTOMER_KYC_SYNCED",
                "BACKOFFICE",
                "CUSTOMER_PROFILE",
                profile.getId(),
                auditMetadata(profile.getCoreCustomerId())
        );

        return toDetailResponse(profile);
    }

    private BackofficeCustomerSummaryResponse toResponse(CustomerProfileEntity profile) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = resolvePrimaryContact(iamUser, ContactType.PHONE);
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);
        ChannelAccessState internetAccess = resolveChannelAccess(iamUser, Channel.INTERNET_BANKING);
        ChannelAccessState mobileAccess = resolveChannelAccess(iamUser, Channel.MOBILE_BANKING);

        return BackofficeCustomerSummaryResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getPublicId() : null)
                .clientId(profile.getCoreCustomerId())
                .username(internetAccess.username())
                .fullName(person != null ? person.getFullName() : null)
                .mobile(mobile)
                .email(email)
                .status(iamUser != null && iamUser.getStatus() != null ? iamUser.getStatus().name() : null)
                .accessLocked(internetAccess.locked())
                .internetAccessStatus(internetAccess.status())
                .internetAccessActive(internetAccess.active())
                .internetPasswordSet(internetAccess.credentialSet())
                .internetLocked(internetAccess.locked())
                .mobileAccessStatus(mobileAccess.status())
                .mobileAccessActive(mobileAccess.active())
                .mobilePinSet(mobileAccess.credentialSet())
                .mobileLocked(mobileAccess.locked())
                .verified(profile.getIsVerified())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    private BackofficeCustomerDetailResponse toDetailResponse(CustomerProfileEntity profile) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;
        ChannelAccessState internetAccess = resolveChannelAccess(iamUser, Channel.INTERNET_BANKING);
        ChannelAccessState mobileAccess = resolveChannelAccess(iamUser, Channel.MOBILE_BANKING);

        return BackofficeCustomerDetailResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getPublicId() : null)
                .clientId(profile.getCoreCustomerId())
                .username(internetAccess.username())
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
                .accessLocked(internetAccess.locked())
                .internetAccessStatus(internetAccess.status())
                .internetAccessActive(internetAccess.active())
                .internetPasswordSet(internetAccess.credentialSet())
                .internetLocked(internetAccess.locked())
                .mobileAccessStatus(mobileAccess.status())
                .mobileAccessActive(mobileAccess.active())
                .mobilePinSet(mobileAccess.credentialSet())
                .mobileLocked(mobileAccess.locked())
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

    private String resolveUsername(IamUserEntity iamUser, Channel channel) {
        if (iamUser == null || channel == null) {
            return null;
        }
        return loginIdentifierRepository
                .findByIamUserAndChannelAndIdentifierType(iamUser, channel, "username")
                .map(LoginIdentifierEntity::getIdentifier)
                .orElse(null);
    }

    private ChannelAccessState resolveChannelAccess(IamUserEntity iamUser, Channel channel) {
        if (iamUser == null || iamUser.getId() == null || channel == null) {
            return ChannelAccessState.empty();
        }

        LoginIdentifierEntity identifier = loginIdentifierRepository
                .findFirstByIamUserAndChannelOrderByIdAsc(iamUser, channel)
                .orElse(null);
        CustomerAuthEntity auth = customerAuthRepository.findByIamUserId(iamUser.getId()).orElse(null);
        boolean credentialSet = switch (channel) {
            case INTERNET_BANKING -> auth != null && StringUtils.hasText(auth.getInternetPasswordHash());
            case MOBILE_BANKING -> auth != null && StringUtils.hasText(auth.getMobilePinHash());
            default -> false;
        };
        boolean locked = switch (channel) {
            case INTERNET_BANKING -> auth != null && Boolean.TRUE.equals(auth.getInternetLocked());
            case MOBILE_BANKING -> auth != null && Boolean.TRUE.equals(auth.getMobileLocked());
            default -> false;
        };
        String status = identifier != null && identifier.getStatus() != null ? identifier.getStatus().name() : "NOT_CONFIGURED";
        boolean active = identifier != null && identifier.getStatus() == IamStatus.ACTIVE && credentialSet && !locked;
        return new ChannelAccessState(
                identifier != null ? identifier.getIdentifier() : null,
                status,
                active,
                credentialSet,
                locked
        );
    }

    private LoginIdentifierEntity getOrCreateInternetIdentifier(IamUserEntity iamUser) {
        return loginIdentifierRepository
                .findByIamUserAndChannelAndIdentifierType(iamUser, Channel.INTERNET_BANKING, "username")
                .orElseGet(() -> {
                    LoginIdentifierEntity identifier = new LoginIdentifierEntity();
                    identifier.setIamUser(iamUser);
                    identifier.setChannel(Channel.INTERNET_BANKING);
                    identifier.setIdentifierType("username");
                    identifier.setIdentifier(generateUniqueInternetUsername());
                    identifier.setCreatedAt(OffsetDateTime.now());
                    return identifier;
                });
    }

    private String generateUniqueInternetUsername() {
        return UsernameGeneratorUtil.generateUniqueNumericUsername(
                8,
                username -> loginIdentifierRepository.existsByChannelAndIdentifierTypeAndIdentifier(
                        Channel.INTERNET_BANKING,
                        "username",
                        username
                )
        );
    }

    private CustomerAuthEntity getOrCreateCustomerAuth(IamUserEntity iamUser) {
        return customerAuthRepository.findByIamUserId(iamUser.getId()).orElseGet(() -> {
            CustomerAuthEntity auth = new CustomerAuthEntity();
            auth.setIamUser(iamUser);
            auth.setInternetPasswordAlgo("bcrypt");
            auth.setInternetFirstTimeLogin(true);
            auth.setInternetFailedAttempts((short) 0);
            auth.setInternetLocked(false);
            auth.setMobilePinAlgo("bcrypt");
            auth.setMobileFirstTimeLogin(true);
            auth.setMobileFailedAttempts((short) 0);
            auth.setMobileLocked(false);
            auth.setMfaEnabled(false);
            auth.setCreatedAt(OffsetDateTime.now());
            auth.setUpdatedAt(OffsetDateTime.now());
            return customerAuthRepository.save(auth);
        });
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
        String username = resolveUsername(iamUser, Channel.INTERNET_BANKING);

        try {
            notificationService.sendAdminPasswordResetNotice(email, fullName, clientId, username, temporaryPassword);
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

    private Map<String, Object> auditMetadata(String clientId, Object... keyValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        String normalizedClientId = trimToNull(clientId);
        if (normalizedClientId != null) {
            metadata.put("client_id", normalizedClientId);
        }
        if (keyValues != null) {
            for (int i = 0; i + 1 < keyValues.length; i += 2) {
                Object key = keyValues[i];
                Object value = keyValues[i + 1];
                if (key instanceof String keyText && StringUtils.hasText(keyText) && value != null) {
                    metadata.put(keyText, value);
                }
            }
        }
        return metadata;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ChannelAccessState(
            String username,
            String status,
            boolean active,
            boolean credentialSet,
            boolean locked
    ) {
        static ChannelAccessState empty() {
            return new ChannelAccessState(null, "NOT_CONFIGURED", false, false, false);
        }
    }
}
