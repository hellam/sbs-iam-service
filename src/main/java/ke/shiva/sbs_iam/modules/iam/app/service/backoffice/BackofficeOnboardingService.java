package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import ke.shiva.client.account.AccountBackofficeClient;
import ke.shiva.client.account.dto.request.BackofficeAccountSeedItem;
import ke.shiva.client.account.dto.request.BackofficeAccountSeedRequest;
import ke.shiva.client.account.dto.response.BackofficeCustomerDetailsResponse;
import ke.shiva.client.account.dto.response.GeneralClientAccountsResponse;
import ke.shiva.client.iam.enums.TaskRole;
import ke.shiva.client.notification.v1.enums.ChannelType;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.dto.BackofficeOnboardingCommand;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerAccountResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerLookupResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.EmployeeProfileRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.EmployeeProfileRoleIdEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.EmployeeRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionIdEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginProfiles;
import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.app.service.GeneratedPasswordService;
import ke.shiva.sbs_iam.modules.iam.infra.external.NotificationService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.BranchRepository;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.UsernameGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeOnboardingService {

    private final PartyRepository partyRepository;
    private final PersonRepository personRepository;
    private final IamUserRepository iamUserRepository;
    private final UserContactRepository userContactRepository;
    private final ProfileContactRepository profileContactRepository;
    private final LoginIdentifierRepository loginIdentifierRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeAuthRepository employeeAuthRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final EmployeeProfileRoleRepository employeeProfileRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgRoleRepository orgRoleRepository;
    private final OrgRolePermissionRepository orgRolePermissionRepository;
    private final FeatureRepository featureRepository;
    private final CountryRepository countryRepository;
    private final BranchRepository branchRepository;
    private final GeneratedPasswordService generatedPasswordService;
    private final AccountBackofficeClient accountBackofficeClient;
    private final NotificationService notificationService;

    public BackofficeCustomerLookupResponse lookupCustomer(String clientId) {
        validateCustomer(clientId);

        BackofficeCustomerDetailsResponse response = ensureCustomerClientAllowed(clientId);
        return toLookupResponse(response);
    }

    public BackofficeCustomerLookupResponse lookupEmployee(String clientId) {
        BackofficeCustomerDetailsResponse response = ensureEmployeeClientAllowed(clientId);

        IamUserEntity iamUser = iamUserRepository.findFirstByParty_CoreCustomerId(clientId).orElse(null);
        if (iamUser != null && employeeProfileRepository.findById(iamUser.getId()).isPresent()) {
            throw BaseException.badRequest("Employee already exists for client ID '" + clientId + "'.");
        }

        return toLookupResponse(response);
    }

    public BackofficeCustomerLookupResponse lookupOrganization(String clientId) {
        PartyEntity party = partyRepository.findByCoreCustomerId(clientId).orElse(null);
        if (party != null) {
            if (party.getPartyType() == PartyType.ORGANIZATION) {
                throw BaseException.badRequest("Client ID '" + clientId + "' is already registered.");
            }
            throw BaseException.badRequest("Individual clients must be onboarded via customer route.");
        }

        BackofficeCustomerDetailsResponse response = ensureOrganizationClientAllowed(clientId);
        return toLookupResponse(response);
    }

    public List<BackofficeCustomerAccountResponse> lookupCustomerAccounts(String clientId, String query) {
        ensureCustomerClientAllowed(clientId);

        List<GeneralClientAccountsResponse> accounts =
                accountBackofficeClient.getClientAccounts(clientId).orElse(List.of());

        return accounts.stream()
                .filter(account -> account.getAccountNumber() != null && !account.getAccountNumber().isBlank())
                .filter(account -> query == null || query.isBlank()
                        || account.getAccountNumber().toLowerCase().contains(query.toLowerCase()))
                .map(account -> new BackofficeCustomerAccountResponse(account.getAccountNumber()))
                .toList();
    }

    public BackofficeCustomerDetailsResponse fetchCustomerCoreDetails(String clientId) {
        return ensureCustomerClientAllowed(clientId);
    }

    public BackofficeCustomerDetailsResponse fetchEmployeeCoreDetails(String clientId) {
        return ensureEmployeeClientAllowed(clientId);
    }

    public BackofficeCustomerDetailsResponse fetchOrganizationCoreDetails(String clientId) {
        return ensureOrganizationClientAllowed(clientId);
    }

    @Transactional
    public BackofficeCustomerOnboardingResponse createCustomer(BackofficeOnboardingCommand request) {
        validateCustomer(request.getClientId());

        BackofficeCustomerDetailsResponse coreDetails = ensureCustomerClientAllowed(request.getClientId());

        String[] names = resolvePersonNames(coreDetails);
        String firstName = names[0];
        String middleName = names[1];
        String lastName = names[2];

        String nationalId = trimToNull(coreDetails.getNationalId());
        if (nationalId == null) {
            throw BaseException.badRequest("National ID is required from core banking.");
        }

        String mobile = trimToNull(coreDetails.getMobile());
        if (mobile == null) {
            throw BaseException.badRequest("Mobile number is required from core banking.");
        }

        String email = trimToNull(coreDetails.getEmail());
        if (email == null) {
            throw BaseException.badRequest("Email is required from core banking.");
        }

        PartyEntity existingParty = partyRepository.findByCoreCustomerId(request.getClientId()).orElse(null);
        if (existingParty != null && existingParty.getPartyType() != PartyType.PERSON) {
            throw BaseException.badRequest("Client ID '" + request.getClientId() + "' is not an individual.");
        }

        IamUserEntity iamUser = iamUserRepository.findFirstByParty_CoreCustomerId(request.getClientId())
                .orElse(null);

        if (iamUser == null) {
            validateCustomerUniqueness(nationalId, mobile, email);

            PartyEntity party = existingParty != null
                    ? existingParty
                    : createParty(PartyType.PERSON, request.getClientId());

            if (party.getCoreCustomerId() == null || party.getCoreCustomerId().isBlank()) {
                party.setCoreCustomerId(request.getClientId());
                partyRepository.save(party);
            }

            if (party.getPerson() == null) {
                createPerson(
                        party,
                        firstName,
                        middleName,
                        lastName,
                        nationalId,
                        resolveCountryInput(coreDetails),
                        coreDetails.getCity(),
                        coreDetails.getAddress1(),
                        null,
                        null
                );
            }

            iamUser = createIamUser(party);
        } else {
            PartyEntity party = iamUser.getParty();
            if (party == null || party.getPartyType() != PartyType.PERSON) {
                throw BaseException.badRequest("Client ID '" + request.getClientId() + "' is not an individual.");
            }

            PersonEntity person = party.getPerson();
            if (person == null) {
                if (personRepository.existsByNationalId(nationalId)) {
                    throw BaseException.badRequest("National ID '" + nationalId + "' is already registered.");
                }
                createPerson(
                        party,
                        firstName,
                        middleName,
                        lastName,
                        nationalId,
                        resolveCountryInput(coreDetails),
                        coreDetails.getCity(),
                        coreDetails.getAddress1(),
                        null,
                        null
                );
            } else {
                String existingNationalId = trimToNull(person.getNationalId());
                if (existingNationalId != null && !existingNationalId.equals(nationalId)) {
                    throw BaseException.badRequest("National ID does not match existing profile.");
                }
            }
        }

        if (customerProfileRepository.findByIamUser(iamUser).isPresent()) {
            throw BaseException.badRequest("Client ID '" + request.getClientId() + "' is already registered as customer.");
        }

        UserContact phone = ensureUserContact(iamUser, ContactType.PHONE, mobile);
        UserContact emailContact = ensureUserContact(iamUser, ContactType.EMAIL, email);

        linkProfileContact(iamUser, phone, LoginProfiles.CUSTOMER, ContactType.PHONE);
        linkProfileContact(iamUser, emailContact, LoginProfiles.CUSTOMER, ContactType.EMAIL);

        LoginIdentifierEntity loginIdentifier = loginIdentifierRepository
                .findByIamUserAndChannelAndIdentifierType(iamUser, Channel.INTERNET_BANKING, "username")
                .orElse(null);

        String username;
        if (loginIdentifier == null) {
            username = generateUniqueUsername(Channel.INTERNET_BANKING);
            createLoginIdentifier(iamUser, username, Channel.INTERNET_BANKING);
        } else {
            username = loginIdentifier.getIdentifier();
        }

        CustomerProfileEntity customerProfile = new CustomerProfileEntity();
        customerProfile.setIamUser(iamUser);
        customerProfile.setCoreCustomerId(request.getClientId());
        customerProfile.setSegment("RETAIL");
        customerProfile.setLanguage("en");
        customerProfile.setTimezone("Africa/Nairobi");
        customerProfile.setTheme("light");
        customerProfile.setIsVerified(true);
        customerProfile.setAllowEmail(true);
        customerProfile.setAllowSms(true);
        customerProfile.setAllowPush(false);
        customerProfile.setCreatedAt(LocalDateTime.now());
        customerProfile.setUpdatedAt(LocalDateTime.now());
        customerProfileRepository.save(customerProfile);

        String rawPassword = null;
        if (customerAuthRepository.findByIamUserId(iamUser.getId()).isEmpty()) {
            rawPassword = generatePassword(Channel.INTERNET_BANKING);
            CustomerAuthEntity auth = new CustomerAuthEntity();
            auth.setIamUser(iamUser);
            auth.setInternetPasswordHash(HashUtil.bcrypt(rawPassword));
            auth.setInternetPasswordAlgo("bcrypt");
            auth.setInternetPasswordChangedAt(OffsetDateTime.now());
            auth.setInternetFirstTimeLogin(true);
            auth.setInternetFailedAttempts((short) 0);
            auth.setInternetLocked(false);
            auth.setMobileFirstTimeLogin(true);
            auth.setMobileFailedAttempts((short) 0);
            auth.setMobileLocked(false);
            auth.setMfaEnabled(false);
            customerAuthRepository.save(auth);
        }

        seedSelectedAccountsIfPresent(request.getClientId(), request.getAccounts(), "backoffice");

        BackofficeCustomerOnboardingResponse response = BackofficeCustomerOnboardingResponse.builder()
                .iamUserId(iamUser.getId())
                .username(username)
                .generatedPassword(rawPassword)
                .build();

        sendCustomerOnboardingConfirmation(
                coreDetails,
                firstName,
                middleName,
                lastName,
                request.getClientId(),
                username,
                rawPassword
        );

        return response;
    }

    @Transactional
    public BackofficeEmployeeOnboardingResponse createEmployee(BackofficeOnboardingCommand request) {
        rejectAccountsForRoute(request.getAccounts(), "Employee");
        String staffNo = resolveEmployeeStaffNo(request.getStaffNo(), request.getClientId());
        BranchEntity branch = resolveEmployeeBranch(request.getBranchId());

        BackofficeCustomerDetailsResponse coreDetails = ensureEmployeeClientAllowed(request.getClientId());

        String[] names = resolvePersonNames(coreDetails);
        String firstName = names[0];
        String middleName = names[1];
        String lastName = names[2];

        String nationalId = trimToNull(coreDetails.getNationalId());
        if (nationalId == null) {
            throw BaseException.badRequest("National ID is required from core banking.");
        }

        String mobile = trimToNull(coreDetails.getMobile());
        if (mobile == null) {
            throw BaseException.badRequest("Mobile number is required from core banking.");
        }

        String employeeEmail = trimToNull(coreDetails.getEmail());
        if (employeeEmail == null) {
            throw BaseException.badRequest("Email is required from core banking.");
        }

        IamUserEntity iamUser = iamUserRepository.findFirstByParty_CoreCustomerId(request.getClientId())
                .orElse(null);
        if (iamUser == null && nationalId != null) {
            iamUser = iamUserRepository.findFirstByParty_Person_NationalIdIgnoreCase(nationalId)
                    .orElse(null);
        }

        if (iamUser == null) {
            validateEmployeeOnCreate(request, null, staffNo, nationalId, mobile, employeeEmail);

            PartyEntity party = createParty(PartyType.PERSON, request.getClientId());
            createPerson(party, firstName, middleName, lastName,
                    nationalId, resolveCountryInput(coreDetails), coreDetails.getCity(),
                    coreDetails.getAddress1(), null, null);
            iamUser = createIamUser(party);
        } else {
            PartyEntity party = iamUser.getParty();
            if (party == null || party.getPartyType() != PartyType.PERSON) {
                throw BaseException.badRequest("Client ID '" + request.getClientId() + "' is not an individual.");
            }

            String existingCoreCustomerId = trimToNull(party.getCoreCustomerId());
            if (existingCoreCustomerId == null) {
                party.setCoreCustomerId(request.getClientId());
                partyRepository.save(party);
            }

            validateEmployeeOnCreate(request, iamUser, staffNo, nationalId, mobile, employeeEmail);

            if (party.getPerson() == null) {
                createPerson(party, firstName, middleName, lastName,
                        nationalId, resolveCountryInput(coreDetails), coreDetails.getCity(),
                        coreDetails.getAddress1(), null, null);
            }
        }

        if (employeeProfileRepository.findById(iamUser.getId()).isPresent()) {
            throw BaseException.badRequest("Employee already exists for client ID '" + request.getClientId() + "'.");
        }

        UserContact phone = ensureUserContact(iamUser, ContactType.PHONE, mobile);
        UserContact email = ensureUserContact(iamUser, ContactType.EMAIL, employeeEmail);

        linkProfileContact(iamUser, phone, LoginProfiles.EMPLOYEE, ContactType.PHONE);
        linkProfileContact(iamUser, email, LoginProfiles.EMPLOYEE, ContactType.EMAIL);

        LoginIdentifierEntity loginIdentifier = loginIdentifierRepository
                .findByIamUserAndChannelAndIdentifierType(iamUser, Channel.BACKOFFICE, "username")
                .orElse(null);

        String username = resolveEmployeeUsername(request.getClientId());
        if (loginIdentifier == null) {
            ensureBackofficeUsernameAvailable(username, iamUser);
            createLoginIdentifier(iamUser, username, Channel.BACKOFFICE);
        } else if (!username.equals(loginIdentifier.getIdentifier())) {
            ensureBackofficeUsernameAvailable(username, iamUser);
            loginIdentifier.setIdentifier(username);
            loginIdentifier.setUpdatedAt(OffsetDateTime.now());
            loginIdentifierRepository.save(loginIdentifier);
        } else {
            ensureBackofficeUsernameAvailable(username, iamUser);
        }

        EmployeeProfileEntity employeeProfile = new EmployeeProfileEntity();
        employeeProfile.setIamUser(iamUser);
        employeeProfile.setStaffNo(staffNo);
        employeeProfile.setJobTitle(request.getJobTitle());
        employeeProfile.setDepartment(request.getDepartment());
        employeeProfile.setEmploymentStatus(request.getEmploymentStatus() != null ? request.getEmploymentStatus() : EmploymentStatus.ACTIVE);
        employeeProfile.setBranch(branch.getId());
        employeeProfile.setCreatedAt(OffsetDateTime.now());
        employeeProfile.setUpdatedAt(OffsetDateTime.now());
        employeeProfileRepository.save(employeeProfile);

        String rawPassword = null;
        if (employeeAuthRepository.findByIamUserId(iamUser.getId()).isEmpty()) {
            rawPassword = generatePassword(Channel.BACKOFFICE);
            EmployeeAuthEntity auth = new EmployeeAuthEntity();
            auth.setIamUser(iamUser);
            auth.setStaffPasswordHash(HashUtil.bcrypt(rawPassword));
            auth.setStaffPasswordAlgo("bcrypt");
            auth.setStaffFailedAttempts((short) 0);
            auth.setStaffLocked(false);
            auth.setFirstTimeLogin(true);
            auth.setMfaEnabled(false);
            employeeAuthRepository.save(auth);
        }

        assignEmployeeRoles(employeeProfile, request.getRoleIds());

        return BackofficeEmployeeOnboardingResponse.builder()
                .iamUserId(iamUser.getId())
                .username(username)
                .generatedPassword(rawPassword)
                .build();
    }

    @Transactional
    public BackofficeOrganizationOnboardingResponse createOrganization(BackofficeOnboardingCommand request) {
        rejectAccountsForRoute(request.getAccounts(), "Organization");
        validateOrganization(request.getClientId(), request.getRegistrationNo());

        BackofficeCustomerDetailsResponse coreDetails = ensureOrganizationClientAllowed(request.getClientId());

        String legalName = firstNonBlank(
                coreDetails.getFullName(),
                buildFullName(coreDetails.getFirstName(), coreDetails.getMiddleName(), coreDetails.getLastName())
        );
        if (legalName == null) {
            throw BaseException.badRequest("Legal name is required.");
        }

        String displayName = legalName;
        String customerSegment = "CORPORATE";
        String countryInput = resolveCountryInput(coreDetails);
        String address = trimToNull(coreDetails.getAddress1());
        String city = trimToNull(coreDetails.getCity());
        String companyPhone = trimToNull(coreDetails.getMobile());
        String companyEmail = trimToNull(coreDetails.getEmail());

        PartyEntity party = createParty(PartyType.ORGANIZATION, request.getClientId());
        OrganizationEntity organization = new OrganizationEntity();
        organization.setParty(party);
        organization.setLegalName(legalName);
        organization.setDisplayName(displayName);
        organization.setRegistrationNo(request.getRegistrationNo());
        organization.setCustomerSegment(customerSegment);
        organization.setSmeMode(Boolean.TRUE.equals(request.getIsSme()));
        organization.setCountryCode(resolveCountry(countryInput));
        organization.setAddress(address);
        organization.setCity(city);
        organization.setCompanyPhone(companyPhone);
        organization.setCompanyEmail(companyEmail);
        organization.setContactPersonName(null);
        organization.setContactPersonEmail(null);
        organization.setContactPersonPhone(null);
        organization.setCreatedAt(OffsetDateTime.now());
        organization.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(organization);
        seedDefaultOrganizationRolesAndPermissions(party);

        return BackofficeOrganizationOnboardingResponse.builder()
                .partyId(party.getId())
                .publicId(party.getPublicId())
                .legalName(organization.getLegalName())
                .build();
    }

    private void seedDefaultOrganizationRolesAndPermissions(PartyEntity organizationParty) {
        OrgRoleEntity makerRole = upsertOrganizationRole(
                organizationParty,
                TaskRole.MAKER,
                "Maker",
                "Initiates and submits corporate transactions.",
                true
        );
        OrgRoleEntity checkerRole = upsertOrganizationRole(
                organizationParty,
                TaskRole.CHECKER,
                "Checker",
                "Reviews and verifies submitted corporate transactions.",
                false
        );
        OrgRoleEntity approverRole = upsertOrganizationRole(
                organizationParty,
                TaskRole.APPROVER,
                "Approver",
                "Performs final transaction approval.",
                false
        );

        FeatureEntity internalTransfer = upsertFeature(
                "INTERNAL_TRANSFER",
                "Internal Transfer",
                "Initiate internal account transfers.",
                "Transfers",
                true
        );
        FeatureEntity rtgsTransfer = upsertFeature(
                "RTGS_TRANSFER",
                "RTGS Transfer",
                "Initiate RTGS transfers.",
                "Transfers",
                true
        );
        FeatureEntity swiftTransfer = upsertFeature(
                "SWIFT_TRANSFER",
                "SWIFT Transfer",
                "Initiate SWIFT transfers.",
                "Transfers",
                true
        );
        FeatureEntity approvalQueue = upsertFeature(
                "APPROVAL_QUEUE",
                "Approval Queue",
                "View and action pending approvals.",
                "Approvals",
                false
        );

        upsertRolePermission(makerRole, internalTransfer);
        upsertRolePermission(makerRole, rtgsTransfer);
        upsertRolePermission(makerRole, swiftTransfer);

        upsertRolePermission(checkerRole, internalTransfer);
        upsertRolePermission(checkerRole, rtgsTransfer);
        upsertRolePermission(checkerRole, swiftTransfer);
        upsertRolePermission(checkerRole, approvalQueue);

        upsertRolePermission(approverRole, internalTransfer);
        upsertRolePermission(approverRole, rtgsTransfer);
        upsertRolePermission(approverRole, swiftTransfer);
        upsertRolePermission(approverRole, approvalQueue);
    }

    private OrgRoleEntity upsertOrganizationRole(
            PartyEntity organizationParty,
            TaskRole taskRole,
            String name,
            String description,
            boolean isDefault
    ) {
        OrgRoleEntity role = orgRoleRepository.findByOrganizationPartyAndTaskRole(organizationParty, taskRole)
                .orElseGet(() -> orgRoleRepository
                        .findByOrganizationPartyAndNameIgnoreCase(organizationParty, name)
                        .orElseGet(OrgRoleEntity::new));

        if (role.getId() == null) {
            role.setOrganizationParty(organizationParty);
            role.setTaskRole(taskRole);
            role.setCreatedAt(OffsetDateTime.now());
        }

        role.setName(name);
        role.setDescription(description);
        // TODO: Make per-organization default-role selection configurable from backoffice policy settings.
        role.setIsDefault(isDefault);
        role.setIsActive(true);
        role.setUpdatedAt(OffsetDateTime.now());
        return orgRoleRepository.save(role);
    }

    private FeatureEntity upsertFeature(
            String code,
            String name,
            String description,
            String category,
            boolean isTransaction
    ) {
        FeatureEntity feature = featureRepository.findByCode(code)
                .orElseGet(FeatureEntity::new);

        if (feature.getId() == null) {
            feature.setCode(code);
            feature.setCreatedAt(OffsetDateTime.now());
        }

        feature.setChannel(Channel.INTERNET_BANKING);
        feature.setName(name);
        feature.setDescription(description);
        feature.setCategory(category);
        feature.setEnabled(true);
        feature.setIsTransaction(isTransaction);
        return featureRepository.save(feature);
    }

    private void upsertRolePermission(OrgRoleEntity role, FeatureEntity feature) {
        OrgRolePermissionIdEntity id = new OrgRolePermissionIdEntity();
        id.setOrgRoleId(role.getId());
        id.setFeatureId(feature.getId());

        if (orgRolePermissionRepository.existsById(id)) {
            return;
        }

        OrgRolePermissionEntity rolePermission = new OrgRolePermissionEntity();
        rolePermission.setId(id);
        rolePermission.setOrgRole(role);
        rolePermission.setFeature(feature);
        orgRolePermissionRepository.save(rolePermission);
    }

    private void assignEmployeeRoles(EmployeeProfileEntity employeeProfile, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<EmployeeRoleEntity> roles = employeeRoleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw BaseException.badRequest("One or more employee roles were not found.");
        }

        List<EmployeeProfileRoleEntity> links = roles.stream()
                .map(role -> {
                    EmployeeProfileRoleIdEntity id = new EmployeeProfileRoleIdEntity();
                    id.setEmployeeProfileIamUserId(employeeProfile.getId());
                    id.setEmployeeRoleId(role.getId());

                    EmployeeProfileRoleEntity link = new EmployeeProfileRoleEntity();
                    link.setId(id);
                    link.setEmployeeProfileIamUser(employeeProfile);
                    link.setEmployeeRole(role);
                    link.setAssignedAt(OffsetDateTime.now());
                    return link;
                })
                .collect(Collectors.toList());

        employeeProfileRoleRepository.saveAll(links);
    }

    private PartyEntity createParty(PartyType type, String coreCustomerId) {
        PartyEntity party = new PartyEntity();
        party.setPublicId(UUID.randomUUID());
        party.setPartyType(type);
        party.setStatus("ACTIVE");
        party.setCoreCustomerId(coreCustomerId);
        return partyRepository.save(party);
    }

    private PersonEntity createPerson(PartyEntity party,
                                      String firstName,
                                      String middleName,
                                      String lastName,
                                      String nationalId,
                                      String country,
                                      String city,
                                      String address,
                                      java.time.LocalDate dob,
                                      String gender) {
        PersonEntity person = new PersonEntity();
        person.setParty(party);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setFullName(buildFullName(firstName, middleName, lastName));
        person.setNationalId(nationalId);
        if (country != null) {
            person.setCountryCode(resolveCountry(country));
        }
        person.setCity(city);
        person.setAddress(address);
        person.setDob(dob);
        person.setGender(gender);
        person.setCreatedAt(OffsetDateTime.now());
        person.setUpdatedAt(OffsetDateTime.now());
        return personRepository.save(person);
    }

    private IamUserEntity createIamUser(PartyEntity party) {
        IamUserEntity iamUser = new IamUserEntity();
        iamUser.setPublicId(UUID.randomUUID());
        iamUser.setParty(party);
        iamUser.setAuthProvider("LOCAL");
        iamUser.setStatus(IamStatus.ACTIVE);
        iamUser.setCreatedAt(OffsetDateTime.now());
        iamUser.setUpdatedAt(OffsetDateTime.now());
        return iamUserRepository.save(iamUser);
    }

    private UserContact createUserContact(IamUserEntity iamUser, ContactType type, String value) {
        UserContact contact = new UserContact();
        contact.setIamUser(iamUser);
        contact.setContactType(type);
        contact.setContactValue(value);
        contact.setPrimary(true);
        contact.setCreatedAt(OffsetDateTime.now());
        contact.setUpdatedAt(OffsetDateTime.now());
        return userContactRepository.save(contact);
    }

    private void linkProfileContact(IamUserEntity iamUser, UserContact contact, LoginProfiles profile, ContactType type) {
        ProfileContact profileContact = new ProfileContact();
        profileContact.setIamUser(iamUser);
        profileContact.setUserContact(contact);
        profileContact.setProfileType(profile);
        profileContact.setContactType(type);
        profileContactRepository.save(profileContact);
    }

    private void createLoginIdentifier(IamUserEntity iamUser, String username, Channel channel) {
        LoginIdentifierEntity loginIdentifier = new LoginIdentifierEntity();
        loginIdentifier.setIamUser(iamUser);
        loginIdentifier.setChannel(channel);
        loginIdentifier.setIdentifierType("username");
        loginIdentifier.setIdentifier(username);
        loginIdentifier.setStatus(IamStatus.ACTIVE);
        loginIdentifierRepository.save(loginIdentifier);
    }

    private UserContact ensureUserContact(IamUserEntity iamUser, ContactType type, String value) {
        if (value == null || value.isBlank()) {
            throw BaseException.badRequest(type.name() + " contact value is required.");
        }

        return userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, type)
                .orElseGet(() -> {
                    if (type == ContactType.PHONE) {
                        validatePhoneUnique(value);
                    } else if (type == ContactType.EMAIL) {
                        validateEmailUnique(value);
                    }
                    return createUserContact(iamUser, type, value);
                });
    }

    private String generateUniqueUsername(Channel channel) {
        return UsernameGeneratorUtil.generateUniqueNumericUsername(8,
                username -> loginIdentifierRepository.existsByChannelAndIdentifierTypeAndIdentifier(
                        channel, "username", username));
    }

    private String generatePassword(Channel channel) {
        return generatedPasswordService.generateTemporaryPassword(channel, null);
    }

    private CountryEntity resolveCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }

        String trimmed = country.trim();
        return countryRepository.findByCountryCode(trimmed.toUpperCase())
                .or(() -> countryRepository.findByCountryNameIgnoreCase(trimmed))
                .orElseThrow(() -> BaseException.badRequest("Country not found: " + country));
    }

    private void validatePhoneUnique(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return;
        }
        String mobileNo = mobile.length() > 9 ? mobile.substring(mobile.length() - 9) : mobile;
        if (userContactRepository.existsByContactTypeAndContactValueContaining(ContactType.PHONE, mobileNo)) {
            throw BaseException.badRequest("Phone number '" + mobile + "' is already registered.");
        }
    }

    private void validateEmailUnique(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (userContactRepository.existsByContactTypeAndContactValue(ContactType.EMAIL, email)) {
            throw BaseException.badRequest("Email '" + email + "' is already registered.");
        }
    }

    private void validateCustomerUniqueness(String nationalId, String mobile, String email) {
        if (nationalId != null && !nationalId.isBlank()) {
            if (personRepository.existsByNationalId(nationalId)) {
                throw BaseException.badRequest("National ID '" + nationalId + "' is already registered.");
            }
        }
        validatePhoneUnique(mobile);
        validateEmailUnique(email);
    }

    private void validateCustomer(String clientId) {
        if (customerProfileRepository.findByCoreCustomerId(clientId).isPresent()) {
            throw BaseException.badRequest("Client ID '" + clientId + "' is already registered.");
        }
    }

    private void validateOrganization(String clientId, String registrationNo) {
        if (partyRepository.existsByCoreCustomerId(clientId)) {
            throw BaseException.badRequest("Client ID '" + clientId + "' is already registered.");
        }

        if (registrationNo != null && !registrationNo.isBlank()) {
            if (organizationRepository.existsByRegistrationNo(registrationNo)) {
                throw BaseException.badRequest("Registration number '" + registrationNo + "' is already registered.");
            }
        }
    }

    private void validateEmployeeOnCreate(BackofficeOnboardingCommand request,
                                          IamUserEntity existingUser,
                                          String staffNo,
                                          String nationalId,
                                          String mobile,
                                          String email) {
        if (employeeProfileRepository.existsByStaffNo(staffNo)) {
            throw BaseException.badRequest("Staff number '" + staffNo + "' is already registered.");
        }

        PersonEntity existingPerson = null;
        if (existingUser != null && existingUser.getParty() != null) {
            existingPerson = existingUser.getParty().getPerson();
        }

        if (existingPerson != null && existingPerson.getNationalId() != null
                && !existingPerson.getNationalId().equals(nationalId)) {
            throw BaseException.badRequest("National ID does not match existing profile.");
        }

        boolean shouldCheckNationalId = existingPerson == null
                || existingPerson.getNationalId() == null
                || existingPerson.getNationalId().isBlank();

        if (shouldCheckNationalId && nationalId != null && !nationalId.isBlank()) {
            if (personRepository.existsByNationalId(nationalId)) {
                throw BaseException.badRequest("National ID '" + nationalId + "' is already registered.");
            }
        }

        boolean hasPhone = existingUser != null
                && userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(existingUser, ContactType.PHONE)
                .isPresent();
        boolean hasEmail = existingUser != null
                && userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(existingUser, ContactType.EMAIL)
                .isPresent();

        if (!hasPhone) {
            validatePhoneUnique(mobile);
        }
        if (!hasEmail) {
            validateEmailUnique(email);
        }

        ensureBackofficeUsernameAvailable(resolveEmployeeUsername(request.getClientId()), existingUser);
    }

    private String resolveEmployeeUsername(String clientId) {
        String username = trimToNull(clientId);
        if (username == null) {
            throw BaseException.badRequest("Client ID is required for employee username.");
        }
        return username;
    }

    private void ensureBackofficeUsernameAvailable(String username, IamUserEntity owner) {
        if (username == null || username.isBlank()) {
            throw BaseException.badRequest("Employee username is required.");
        }

        LoginIdentifierEntity existing = loginIdentifierRepository
                .findByIdentifierAndChannel(username, Channel.BACKOFFICE)
                .orElse(null);
        if (existing == null) {
            return;
        }

        Long existingUserId = existing.getIamUser() != null ? existing.getIamUser().getId() : null;
        Long ownerUserId = owner != null ? owner.getId() : null;
        if (ownerUserId != null && ownerUserId.equals(existingUserId)) {
            return;
        }

        throw BaseException.badRequest("Username '" + username + "' is already registered.");
    }

    private void rejectAccountsForRoute(List<String> accounts, String context) {
        if (accounts == null) {
            return;
        }
        boolean hasValues = accounts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(s -> !s.isBlank());
        if (hasValues) {
            throw BaseException.badRequest(context + " onboarding does not accept accounts.");
        }
    }

    private BackofficeCustomerDetailsResponse ensureClientExists(String clientId) {
        BackofficeCustomerDetailsResponse response = accountBackofficeClient.getClientDetails(clientId)
                .orElseThrow(() -> BaseException.notFound("Client ID '" + clientId + "' not found in core banking."));
        if (response.getClientId() == null || response.getClientId().isBlank()) {
            throw BaseException.notFound("Client ID '" + clientId + "' not found in core banking.");
        }
        return response;
    }

    private BackofficeCustomerDetailsResponse ensureCustomerClientAllowed(String clientId) {
        BackofficeCustomerDetailsResponse response = ensureClientExists(clientId);
        String clientTypeId = trimToNull(response.getClientTypeId());
        if (clientTypeId == null) {
            throw BaseException.badRequest("Client type is required from core banking.");
        }
        if (!"I".equalsIgnoreCase(clientTypeId) && !"E".equalsIgnoreCase(clientTypeId)) {
            throw BaseException.badRequest("Only individual customers are allowed.");
        }
        return response;
    }

    private BackofficeCustomerDetailsResponse ensureEmployeeClientAllowed(String clientId) {
        BackofficeCustomerDetailsResponse response = ensureClientExists(clientId);
        String clientTypeId = trimToNull(response.getClientTypeId());
        if (clientTypeId == null) {
            throw BaseException.badRequest("Client type is required from core banking.");
        }
        if (!"I".equalsIgnoreCase(clientTypeId) && !"E".equalsIgnoreCase(clientTypeId)) {
            throw BaseException.badRequest("Corporate clients cannot be onboarded as employees.");
        }
        return response;
    }

    private BranchEntity resolveEmployeeBranch(Long branchId) {
        if (branchId != null) {
            return branchRepository.findById(branchId)
                    .orElseThrow(() -> BaseException.notFound("Branch not found"));
        }

        return branchRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> BaseException.notFound("No branches available for employee onboarding."));
    }

    private String resolveEmployeeStaffNo(String staffNo, String clientId) {
        String resolved = trimToNull(staffNo);
        if (resolved == null) {
            resolved = "EMP-" + clientId;
        }

        if (!employeeProfileRepository.existsByStaffNo(resolved)) {
            return resolved;
        }

        for (int i = 1; i <= 9; i++) {
            String candidate = resolved + "-" + i;
            if (!employeeProfileRepository.existsByStaffNo(candidate)) {
                return candidate;
            }
        }

        throw BaseException.badRequest("Unable to generate a unique staff number.");
    }

    private BackofficeCustomerDetailsResponse ensureOrganizationClientAllowed(String clientId) {
        BackofficeCustomerDetailsResponse response = ensureClientExists(clientId);
        String clientTypeId = trimToNull(response.getClientTypeId());
        if (clientTypeId == null) {
            throw BaseException.badRequest("Client type is required from core banking.");
        }
        if ("I".equalsIgnoreCase(clientTypeId) || "E".equalsIgnoreCase(clientTypeId)) {
            throw BaseException.badRequest("Individual clients must be onboarded via customer route.");
        }
        return response;
    }

    private BackofficeCustomerLookupResponse toLookupResponse(
            BackofficeCustomerDetailsResponse response
    ) {
        String fullName = response.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = buildFullName(response.getFirstName(), response.getMiddleName(), response.getLastName());
        }

        return BackofficeCustomerLookupResponse.builder()
                .clientId(response.getClientId())
                .fullName(fullName)
                .mobile(response.getMobile())
                .email(response.getEmail())
                .country(response.getCountryName())
                .city(response.getCity())
                .openedDate(response.getOpenedDate())
                .address(response.getAddress1())
                .build();
    }

    private void seedSelectedAccountsIfPresent(String clientId, List<String> accountNumbers, String createdBy) {
        if (accountNumbers == null || accountNumbers.isEmpty()) {
            return;
        }

        List<String> distinctNumbers = accountNumbers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        if (distinctNumbers.isEmpty()) {
            return;
        }

        List<GeneralClientAccountsResponse> coreAccounts =
                accountBackofficeClient.getClientAccounts(clientId).orElse(List.of());

        if (coreAccounts.isEmpty()) {
            throw BaseException.badRequest("No accounts found for client ID '" + clientId + "'.");
        }

        Map<String, GeneralClientAccountsResponse> byNumber = coreAccounts.stream()
                .filter(a -> a.getAccountNumber() != null && !a.getAccountNumber().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getAccountNumber().trim(),
                        a -> a,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));

        List<BackofficeAccountSeedItem> seedItems = distinctNumbers.stream()
                .map(num -> {
                    GeneralClientAccountsResponse account = byNumber.get(num);
                    if (account == null) {
                        throw BaseException.badRequest("Account '" + num + "' does not belong to client ID '" + clientId + "'.");
                    }

                    ke.shiva.client.account.dto.response.BackofficeAccountDetailsResponse details = null;
                    if (isBlank(account.getAccountName()) || isBlank(account.getCurrency())) {
                        details = accountBackofficeClient.getAccountDetails(num)
                                .orElseThrow(() -> BaseException.badRequest("Account details not found for '" + num + "'."));

                        if (details.getClientId() != null && !details.getClientId().isBlank()
                                && !details.getClientId().equals(clientId)) {
                            throw BaseException.badRequest("Account '" + num + "' does not belong to client ID '" + clientId + "'.");
                        }
                    }

                    String accountName = firstNonBlank(
                            details != null ? details.getAccountName() : null,
                            account.getAccountName()
                    );
                    String currency = firstNonBlank(
                            details != null ? details.getCurrency() : null,
                            account.getCurrency()
                    );

                    if (isBlank(accountName)) {
                        throw BaseException.badRequest("Account name missing for '" + num + "'.");
                    }
                    if (isBlank(currency)) {
                        throw BaseException.badRequest("Account currency missing for '" + num + "'.");
                    }

                    String branchId = firstNonBlank(
                            details != null ? details.getBranchId() : null,
                            account.getBranchId()
                    );
                    String branchName = firstNonBlank(
                            details != null ? details.getBranchName() : null,
                            account.getBranchName()
                    );
                    String productId = firstNonBlank(
                            details != null ? details.getProductId() : null,
                            account.getProductId()
                    );
                    String productName = firstNonBlank(
                            details != null ? details.getProductName() : null,
                            account.getProductName()
                    );
                    String iban = firstNonBlank(
                            details != null ? details.getIban() : null,
                            account.getIban()
                    );
                    String mobile = firstNonBlank(
                            details != null ? details.getMobile() : null,
                            account.getMobile()
                    );
                    String email = firstNonBlank(
                            details != null ? details.getEmail() : null,
                            account.getEmail()
                    );

                    return BackofficeAccountSeedItem.builder()
                            .accountNumber(account.getAccountNumber())
                            .accountName(accountName)
                            .currency(currency)
                            .iban(iban)
                            .branchId(branchId)
                            .branchName(branchName)
                            .phone(mobile)
                            .email(email)
                            .productId(productId)
                            .productName(productName)
                            .build();
                })
                .toList();

        if (!seedItems.isEmpty()) {
            seedItems.get(0).setPrimary(true);
        }

        BackofficeAccountSeedRequest seedRequest = BackofficeAccountSeedRequest.builder()
                .clientId(Long.parseLong(clientId))
                .accounts(seedItems)
                .createdBy(createdBy)
                .build();

        accountBackofficeClient.seedClientAccounts(seedRequest);
    }

    private void sendCustomerOnboardingConfirmation(
            BackofficeCustomerDetailsResponse coreDetails,
            String firstName,
            String middleName,
            String lastName,
            String clientId,
            String username,
            String generatedPassword
    ) {
        String recipient = trimToNull(coreDetails.getEmail());
        if (isBlank(recipient)) {
            log.warn("Skipping customer onboarding confirmation for client {}: email not available", clientId);
            return;
        }
        String customerName = firstNonBlank(
                trimToNull(coreDetails.getFullName()),
                buildFullName(firstName, middleName, lastName),
                "Customer"
        );

        Map<String, Object> additionalInfo = new java.util.LinkedHashMap<>();
        additionalInfo.put("clientId", clientId);
        additionalInfo.put("onboardingType", "CUSTOMER");
        additionalInfo.put("name", customerName);
        if (!isBlank(username)) {
            additionalInfo.put("userName", username);
            additionalInfo.put("username", username);
        }
        if (!isBlank(generatedPassword)) {
            additionalInfo.put("password", generatedPassword);
        }

        try {
            String effectiveRecipient = notificationService.resolveDeliveryRecipient(ChannelType.EMAIL, recipient);
            notificationService.sendWelcomeMessage(
                    ChannelType.EMAIL,
                    recipient,
                    customerName,
                    additionalInfo
            );
            log.info("Customer onboarding confirmation queued to {}", effectiveRecipient);
        } catch (Exception exception) {
            log.warn(
                    "Customer onboarding confirmation failed for client {}: {}",
                    clientId,
                    exception.getMessage()
            );
        }
    }

    private String resolveCountryInput(BackofficeCustomerDetailsResponse response) {
        if (response == null) {
            return null;
        }
        String countryId = trimToNull(response.getCountryId());
        if (countryId != null) {
            return countryId;
        }
        return trimToNull(response.getCountryName());
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
            if (parts.length > 2) {
                middle = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
            } else {
                middle = null;
            }
        }

        return new String[]{first, middle, last};
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder builder = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(lastName.trim());
        }
        return builder.toString();
    }
}
