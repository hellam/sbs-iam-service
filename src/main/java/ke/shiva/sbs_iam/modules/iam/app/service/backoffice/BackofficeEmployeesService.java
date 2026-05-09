package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.client.account.dto.response.BackofficeCustomerDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeDetailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.GeneratedPasswordService;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordUpdateService;
import ke.shiva.sbs_iam.modules.iam.app.service.SessionRevocationService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.external.NotificationService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.BranchRepository;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import ke.shiva.shivacorestarter.util.PaginationUtil;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeEmployeesService {

    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeAuthRepository employeeAuthRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final UserContactRepository userContactRepository;
    private final BranchRepository branchRepository;
    private final CountryRepository countryRepository;
    private final LoginIdentifierRepository loginIdentifierRepository;
    private final IamUserRepository iamUserRepository;
    private final BackofficeAuditTrailService auditTrailService;
    private final PasswordUpdateService passwordUpdateService;
    private final SessionRevocationService sessionRevocationService;
    private final NotificationService notificationService;
    private final GeneratedPasswordService generatedPasswordService;
    private final BackofficeOnboardingService onboardingService;
    private final EncryptionUtil encryptionUtil;

    @Transactional(readOnly = true)
    public PaginatedResponse<BackofficeEmployeeSummaryResponse> getEmployees(HttpServletRequest request) {
        validateFilters(request);

        List<String> searchableColumns = List.of(
                "iamUser.party.coreCustomerId",
                "iamUser.party.person.fullName",
                "staffNo",
                "jobTitle",
                "department",
                "iamUser.contacts.contactValue"
        );
        List<String> sortableColumns = List.of(
                "createdAt",
                "updatedAt",
                "staffNo",
                "employmentStatus",
                "branch",
                "iamUser.status",
                "iamUser.party.coreCustomerId",
                "iamUser.party.person.fullName"
        );
        List<String> filterableColumns = List.of(
                "employmentStatus",
                "branch",
                "iamUser.status"
        );

        Page<EmployeeProfileEntity> page = PaginationUtil.filterAndPaginateWithScoping(
                employeeProfileRepository,
                request,
                searchableColumns,
                sortableColumns,
                filterableColumns,
                "iamUser.party.partyType",
                PartyType.PERSON.name(),
                10
        );

        Set<Long> branchIds = page.getContent().stream()
                .map(EmployeeProfileEntity::getBranch)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> branchNamesById = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getBranchName, (existing, ignored) -> existing));

        Page<BackofficeEmployeeSummaryResponse> dtoPage = page.map(entity -> toResponse(entity, branchNamesById));
        return PaginationUtil.toPaginatedResponse(dtoPage);
    }

    @Transactional(readOnly = true)
    public List<BackofficeOrganizationUserResponse> getOrganizationUsersByClientId(String clientId) {
        String normalizedClientId = StringUtils.hasText(clientId) ? clientId.trim() : null;
        if (!StringUtils.hasText(normalizedClientId)) {
            throw BaseException.badRequest("Client ID is required.");
        }

        List<OrganizationUserEntity> organizationUsers =
                organizationUserRepository.findByOrganizationParty_CoreCustomerIdOrderByCreatedAtDesc(normalizedClientId);

        return organizationUsers.stream()
                .map(this::toOrganizationUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BackofficeEmployeeDetailResponse getEmployee(String clientId) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        return toDetailResponse(profile);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BackofficeAuditTrailResponse> getEmployeeAuditTrail(
            String clientId,
            HttpServletRequest request
    ) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            return auditTrailService.getUserAuditTrail(null, null, request);
        }

        return auditTrailService.getUserAuditTrail(iamUser, null, request);
    }

    @Transactional
    public BackofficeEmployeeDetailResponse updateEmployeeStatus(String clientId, IamStatus status) {
        if (status == null) {
            throw BaseException.badRequest("status is required.");
        }

        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null) {
            throw BaseException.notFound("IAM user not found for employee.");
        }

        iamUser.setStatus(status);
        iamUser.setUpdatedAt(OffsetDateTime.now());
        iamUserRepository.save(iamUser);
        auditTrailService.recordUserAudit(
                iamUser,
                "EMPLOYEE",
                "BACKOFFICE_EMPLOYEE_STATUS_UPDATED",
                "BACKOFFICE",
                "EMPLOYEE_PROFILE",
                profile.getId(),
                auditMetadata(profile, "status", status.name())
        );

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeEmployeeDetailResponse updateEmployeeAccessLock(String clientId, boolean blocked) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.notFound("IAM user not found for employee.");
        }

        EmployeeAuthEntity auth = employeeAuthRepository.findByIamUserId(iamUser.getId())
                .orElseThrow(() -> BaseException.notFound("Employee credentials not found."));

        auth.setStaffLocked(blocked);
        if (blocked) {
            auth.setStaffLockoutUntil(null);
        } else {
            auth.setStaffLockoutUntil(null);
            auth.setStaffFailedAttempts((short) 0);
        }
        employeeAuthRepository.save(auth);

        if (blocked) {
            sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_EMPLOYEE_BLOCKED");
        }
        auditTrailService.recordUserAudit(
                iamUser,
                "EMPLOYEE",
                blocked ? "BACKOFFICE_EMPLOYEE_ACCESS_BLOCKED" : "BACKOFFICE_EMPLOYEE_ACCESS_UNBLOCKED",
                "BACKOFFICE",
                "EMPLOYEE_PROFILE",
                profile.getId(),
                auditMetadata(profile, "blocked", blocked)
        );

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeEmployeeDetailResponse resetEmployeePassword(String clientId) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.notFound("IAM user not found for employee.");
        }

        String randomPassword = generatedPasswordService.generateTemporaryPassword(Channel.BACKOFFICE, 16);
        passwordUpdateService.updatePassword(iamUser, randomPassword, Channel.BACKOFFICE, true);
        sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_EMPLOYEE_PASSWORD_RESET");
        sendPasswordResetNotification(iamUser, profile.getStaffNo(), randomPassword);
        auditTrailService.recordUserAudit(
                iamUser,
                "EMPLOYEE",
                "BACKOFFICE_EMPLOYEE_PASSWORD_RESET",
                "BACKOFFICE",
                "EMPLOYEE_PROFILE",
                profile.getId(),
                auditMetadata(profile)
        );

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeEmployeeDetailResponse resetEmployeeMfa(String clientId) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.notFound("IAM user not found for employee.");
        }

        EmployeeAuthEntity auth = employeeAuthRepository.findByIamUserId(iamUser.getId())
                .orElseThrow(() -> BaseException.notFound("Employee credentials not found."));
        if (!isMfaTotpConfigured(auth)) {
            throw BaseException.badRequest("MFA TOTP is not configured for this user.");
        }
        auth.setMfaEnabled(false);
        auth.setMfaSecret(null);
        auth.setMfaLastVerifiedAt(null);
        employeeAuthRepository.save(auth);
        auditTrailService.recordUserAudit(
                iamUser,
                "EMPLOYEE",
                "BACKOFFICE_EMPLOYEE_MFA_RESET",
                "BACKOFFICE",
                "EMPLOYEE_PROFILE",
                profile.getId(),
                auditMetadata(profile)
        );

        return toDetailResponse(profile);
    }

    @Transactional
    public BackofficeEmployeeDetailResponse syncEmployeeKyc(String clientId) {
        EmployeeProfileEntity profile = getRequiredEmployeeProfile(clientId);
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;
        if (iamUser == null || party == null || person == null) {
            throw BaseException.notFound("Employee profile is incomplete.");
        }

        BackofficeCustomerDetailsResponse coreDetails = onboardingService.fetchEmployeeCoreDetails(party.getCoreCustomerId());
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
                "EMPLOYEE",
                "BACKOFFICE_EMPLOYEE_KYC_SYNCED",
                "BACKOFFICE",
                "EMPLOYEE_PROFILE",
                profile.getId(),
                auditMetadata(profile)
        );

        return toDetailResponse(profile);
    }

    private BackofficeEmployeeSummaryResponse toResponse(EmployeeProfileEntity profile, Map<Long, String> branchNamesById) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = resolvePrimaryContact(iamUser, ContactType.PHONE);
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);

        return BackofficeEmployeeSummaryResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getId() : null)
                .clientId(party != null ? party.getCoreCustomerId() : null)
                .username(resolveUsername(iamUser, Channel.BACKOFFICE))
                .fullName(person != null ? person.getFullName() : null)
                .staffNo(profile.getStaffNo())
                .mobile(mobile)
                .email(email)
                .jobTitle(profile.getJobTitle())
                .department(profile.getDepartment())
                .employmentStatus(profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : null)
                .branchId(profile.getBranch())
                .branchName(profile.getBranch() != null ? branchNamesById.get(profile.getBranch()) : null)
                .status(iamUser != null && iamUser.getStatus() != null ? iamUser.getStatus().name() : null)
                .accessLocked(isEmployeeAccessLocked(iamUser))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private BackofficeEmployeeDetailResponse toDetailResponse(EmployeeProfileEntity profile) {
        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        Map<Long, String> branchNameMap = branchRepository.findAllById(
                profile.getBranch() == null ? Set.<Long>of() : Set.of(profile.getBranch())
        ).stream().collect(Collectors.toMap(BranchEntity::getId, BranchEntity::getBranchName, (existing, ignored) -> existing));

        return BackofficeEmployeeDetailResponse.builder()
                .iamUserId(iamUser != null ? iamUser.getId() : null)
                .clientId(party != null ? party.getCoreCustomerId() : null)
                .username(resolveUsername(iamUser, Channel.BACKOFFICE))
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
                .accessLocked(isEmployeeAccessLocked(iamUser))
                .mfaTotpEnabled(isEmployeeMfaTotpEnabled(iamUser))
                .staffNo(profile.getStaffNo())
                .jobTitle(profile.getJobTitle())
                .department(profile.getDepartment())
                .employmentStatus(profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : null)
                .branchId(profile.getBranch())
                .branchName(profile.getBranch() != null ? branchNameMap.get(profile.getBranch()) : null)
                .lastLoginAt(iamUser != null ? iamUser.getLastLoginAt() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private void validateFilters(HttpServletRequest request) {
        String iamStatus = firstNonBlank(
                request.getParameter("status"),
                request.getParameter("iamUser.status"),
                request.getParameter("iamUser_status")
        );
        if (StringUtils.hasText(iamStatus)) {
            try {
                IamStatus.valueOf(iamStatus.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw BaseException.badRequest("Invalid status. Allowed values: ACTIVE, INACTIVE, LOCKED, SUSPENDED.");
            }
        }

        String employmentStatus = firstNonBlank(request.getParameter("employmentStatus"));
        if (StringUtils.hasText(employmentStatus)) {
            try {
                EmploymentStatus.valueOf(employmentStatus.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw BaseException.badRequest("Invalid employmentStatus. Allowed values: ACTIVE, SUSPENDED, TERMINATED, ON_LEAVE.");
            }
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

    private EmployeeProfileEntity getRequiredEmployeeProfile(String clientId) {
        String normalizedClientId = StringUtils.hasText(clientId) ? clientId.trim() : null;
        if (!StringUtils.hasText(normalizedClientId)) {
            throw BaseException.badRequest("Client ID is required.");
        }

        EmployeeProfileEntity profile = employeeProfileRepository.findFirstByIamUser_Party_CoreCustomerId(normalizedClientId)
                .orElseThrow(() -> BaseException.notFound("Employee " + normalizedClientId + " not found."));

        IamUserEntity iamUser = profile.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        if (party == null || party.getPartyType() != PartyType.PERSON) {
            throw BaseException.notFound("Employee " + normalizedClientId + " is not a person profile.");
        }

        return profile;
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

    private BackofficeOrganizationUserResponse toOrganizationUserResponse(OrganizationUserEntity entity) {
        IamUserEntity iamUser = entity.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String mobile = null;
        String email = null;

        if (iamUser != null && iamUser.getContacts() != null) {
            mobile = iamUser.getContacts().stream()
                    .filter(contact -> contact.getContactType() == ContactType.PHONE)
                    .filter(UserContact::isPrimary)
                    .map(UserContact::getContactValue)
                    .findFirst()
                    .orElseGet(() -> iamUser.getContacts().stream()
                            .filter(contact -> contact.getContactType() == ContactType.PHONE)
                            .map(UserContact::getContactValue)
                            .findFirst()
                            .orElse(null));

            email = iamUser.getContacts().stream()
                    .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                    .filter(UserContact::isPrimary)
                    .map(UserContact::getContactValue)
                    .findFirst()
                    .orElseGet(() -> iamUser.getContacts().stream()
                            .filter(contact -> contact.getContactType() == ContactType.EMAIL)
                            .map(UserContact::getContactValue)
                            .findFirst()
                            .orElse(null));
        }

        String roleName = entity.getOrgRole() != null ? entity.getOrgRole().getName() : null;
        String taskRole = entity.getOrgRole() != null && entity.getOrgRole().getTaskRole() != null
                ? entity.getOrgRole().getTaskRole().name()
                : null;

        return BackofficeOrganizationUserResponse.builder()
                .organizationUserRef(encryptPositiveLongId(entity.getId(), "organizationUserId"))
                .iamUserRef(iamUser != null ? encryptPositiveLongId(iamUser.getId(), "iamUserId") : null)
                .individualClientId(party != null ? party.getCoreCustomerId() : null)
                .clientId(entity.getOrganizationParty() != null ? entity.getOrganizationParty().getCoreCustomerId() : null)
                .fullName(person != null ? person.getFullName() : null)
                .mobile(mobile)
                .email(email)
                .verified(iamUser != null
                        && iamUser.getCustomerProfile() != null
                        && Boolean.TRUE.equals(iamUser.getCustomerProfile().getIsVerified()))
                .internetLocked(iamUser != null
                        && iamUser.getCustomerAuth() != null
                        && Boolean.TRUE.equals(iamUser.getCustomerAuth().getInternetLocked()))
                .mfaTotpEnabled(isOrganizationUserMfaTotpEnabled(iamUser))
                .roleName(roleName)
                .taskRole(taskRole)
                .primary(entity.getIsPrimary())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String encryptPositiveLongId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            return null;
        }

        try {
            return encryptionUtil.encrypt(String.valueOf(value));
        } catch (Exception exception) {
            log.error("Unable to encrypt {} for backoffice response: {}", fieldName, exception.getMessage(), exception);
            throw BaseException.badRequest("Unable to process " + fieldName + ".");
        }
    }

    private boolean isEmployeeAccessLocked(IamUserEntity iamUser) {
        if (iamUser == null || iamUser.getId() == null) {
            return false;
        }
        return employeeAuthRepository.findByIamUserId(iamUser.getId())
                .map(EmployeeAuthEntity::getStaffLocked)
                .orElse(false);
    }

    private boolean isEmployeeMfaTotpEnabled(IamUserEntity iamUser) {
        if (iamUser == null || iamUser.getId() == null) {
            return false;
        }
        return employeeAuthRepository.findByIamUserId(iamUser.getId())
                .map(this::isMfaTotpConfigured)
                .orElse(false);
    }

    private boolean isOrganizationUserMfaTotpEnabled(IamUserEntity iamUser) {
        if (iamUser == null || iamUser.getId() == null) {
            return false;
        }
        return customerAuthRepository.findByIamUserId(iamUser.getId())
                .map(this::isMfaTotpConfigured)
                .orElse(false);
    }

    private boolean isMfaTotpConfigured(EmployeeAuthEntity auth) {
        return auth != null
                && Boolean.TRUE.equals(auth.getMfaEnabled())
                && StringUtils.hasText(auth.getMfaSecret());
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

    private void sendPasswordResetNotification(IamUserEntity iamUser, String staffNo, String temporaryPassword) {
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);
        if (!StringUtils.hasText(email)) {
            return;
        }

        String fullName = iamUser.getParty() != null && iamUser.getParty().getPerson() != null
                ? iamUser.getParty().getPerson().getFullName()
                : "Employee";

        try {
            notificationService.sendAdminPasswordResetNotice(email, fullName, staffNo, temporaryPassword);
        } catch (Exception exception) {
            log.warn("Password reset notification failed for employee {}: {}", staffNo, exception.getMessage());
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

    private Map<String, Object> auditMetadata(EmployeeProfileEntity profile, Object... keyValues) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        String clientId = resolveProfileClientId(profile);
        if (clientId != null) {
            metadata.put("client_id", clientId);
        }
        String staffNo = trimToNull(profile != null ? profile.getStaffNo() : null);
        if (staffNo != null) {
            metadata.put("staff_no", staffNo);
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

    private String resolveProfileClientId(EmployeeProfileEntity profile) {
        IamUserEntity iamUser = profile != null ? profile.getIamUser() : null;
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        return trimToNull(party != null ? party.getCoreCustomerId() : null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
