package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import ke.shiva.client.account.AccountBackofficeClient;
import ke.shiva.client.account.dto.request.BackofficeAccountSeedItem;
import ke.shiva.client.account.dto.request.BackofficeAccountSeedRequest;
import ke.shiva.client.account.dto.response.BackofficeCustomerDetailsResponse;
import ke.shiva.client.account.dto.response.GeneralClientAccountsResponse;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.*;
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
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginProfiles;
import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordPolicyService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.BranchRepository;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.PasswordGeneratorUtil;
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
    private final OrganizationUserRepository organizationUserRepository;
    private final CountryRepository countryRepository;
    private final BranchRepository branchRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final AccountBackofficeClient accountBackofficeClient;

    public void validateCustomer(BackofficeCustomerValidationRequest request) {
        if (customerProfileRepository.findByCoreCustomerId(request.getClientId()).isPresent()) {
            throw BaseException.badRequest("Client ID '" + request.getClientId() + "' is already registered.");
        }
    }

    public BackofficeCustomerLookupResponse lookupCustomer(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw BaseException.badRequest("Client ID is required.");
        }

        validateCustomer(BackofficeCustomerValidationRequest.builder()
                .clientId(clientId)
                .build());

        BackofficeCustomerDetailsResponse response = ensureIndividualClientExists(clientId);
        return toLookupResponse(response);
    }

    public List<BackofficeCustomerAccountResponse> lookupCustomerAccounts(
            String clientId,
            String query
    ) {
        ensureIndividualClientExists(clientId);

        List<GeneralClientAccountsResponse> accounts =
                accountBackofficeClient.getClientAccounts(clientId).orElse(List.of());

        return accounts.stream()
                .filter(account -> account.getAccountNumber() != null && !account.getAccountNumber().isBlank())
                .filter(account -> query == null || query.isBlank()
                        || account.getAccountNumber().toLowerCase().contains(query.toLowerCase()))
                .map(account -> new BackofficeCustomerAccountResponse(account.getAccountNumber()))
                .toList();
    }

    @Transactional
    public BackofficeCustomerOnboardingResponse createCustomer(BackofficeCustomerOnboardingRequest request) {
        validateCustomer(BackofficeCustomerValidationRequest.builder()
                .clientId(request.getClientId())
                .build());

        BackofficeCustomerDetailsResponse coreDetails = ensureIndividualClientExists(request.getClientId());

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

        validateCustomerUniqueness(nationalId, mobile, email);

        PartyEntity party = createParty(PartyType.PERSON, request.getClientId());
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
        IamUserEntity iamUser = createIamUser(party);

        UserContact phone = createUserContact(iamUser, ContactType.PHONE, mobile);
        UserContact emailContact = createUserContact(iamUser, ContactType.EMAIL, email);

        linkProfileContact(iamUser, phone, LoginProfiles.CUSTOMER, ContactType.PHONE);
        linkProfileContact(iamUser, emailContact, LoginProfiles.CUSTOMER, ContactType.EMAIL);

        String username = generateUniqueUsername(Channel.INTERNET_BANKING);
        createLoginIdentifier(iamUser, username, Channel.INTERNET_BANKING);

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

        String rawPassword = generatePassword(Channel.INTERNET_BANKING);
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

        seedSelectedAccountsIfPresent(request.getClientId(), request.getAccounts(), "backoffice");

        return BackofficeCustomerOnboardingResponse.builder()
                .iamUserId(iamUser.getId())
                .username(username)
                .generatedPassword(rawPassword)
                .build();
    }

    public void validateEmployee(BackofficeEmployeeValidationRequest request) {
        if (employeeProfileRepository.existsByStaffNo(request.getStaffNo())) {
            throw BaseException.badRequest("Staff number '" + request.getStaffNo() + "' is already registered.");
        }

        if (personRepository.existsByNationalId(request.getNationalId())) {
            throw BaseException.badRequest("National ID '" + request.getNationalId() + "' is already registered.");
        }

        validatePhoneUnique(request.getMobile());
        validateEmailUnique(request.getEmail());

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (loginIdentifierRepository.existsByChannelAndIdentifierTypeAndIdentifier(
                    Channel.BACKOFFICE, "username", request.getUsername())) {
                throw BaseException.badRequest("Username '" + request.getUsername() + "' is already registered.");
            }
        }
    }

    @Transactional
    public BackofficeEmployeeOnboardingResponse createEmployee(BackofficeEmployeeOnboardingRequest request) {
        validateEmployee(BackofficeEmployeeValidationRequest.builder()
                .staffNo(request.getStaffNo())
                .nationalId(request.getNationalId())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .username(request.getUsername())
                .build());

        ensureClientExists(request.getClientId());

        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> BaseException.notFound("Branch not found"));

        PartyEntity party = createParty(PartyType.PERSON, request.getClientId());
        createPerson(party, request.getFirstName(), request.getMiddleName(), request.getLastName(),
                request.getNationalId(), request.getCountry(), request.getCity(), request.getAddress(), request.getDob(), request.getGender());
        IamUserEntity iamUser = createIamUser(party);

        UserContact phone = createUserContact(iamUser, ContactType.PHONE, request.getMobile());
        UserContact email = createUserContact(iamUser, ContactType.EMAIL, request.getEmail());

        linkProfileContact(iamUser, phone, LoginProfiles.EMPLOYEE, ContactType.PHONE);
        linkProfileContact(iamUser, email, LoginProfiles.EMPLOYEE, ContactType.EMAIL);

        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = generateUniqueUsername(Channel.BACKOFFICE);
        }
        createLoginIdentifier(iamUser, username, Channel.BACKOFFICE);

        EmployeeProfileEntity employeeProfile = new EmployeeProfileEntity();
        employeeProfile.setIamUser(iamUser);
        employeeProfile.setStaffNo(request.getStaffNo());
        employeeProfile.setJobTitle(request.getJobTitle());
        employeeProfile.setDepartment(request.getDepartment());
        employeeProfile.setEmploymentStatus(request.getEmploymentStatus() != null ? request.getEmploymentStatus() : EmploymentStatus.ACTIVE);
        employeeProfile.setBranch(branch.getId());
        employeeProfile.setCreatedAt(OffsetDateTime.now());
        employeeProfile.setUpdatedAt(OffsetDateTime.now());
        employeeProfileRepository.save(employeeProfile);

        String rawPassword = generatePassword(Channel.BACKOFFICE);
        EmployeeAuthEntity auth = new EmployeeAuthEntity();
        auth.setIamUser(iamUser);
        auth.setStaffPasswordHash(HashUtil.bcrypt(rawPassword));
        auth.setStaffPasswordAlgo("bcrypt");
        auth.setStaffFailedAttempts((short) 0);
        auth.setStaffLocked(false);
        auth.setFirstTimeLogin(true);
        auth.setMfaEnabled(false);
        employeeAuthRepository.save(auth);

        assignEmployeeRoles(employeeProfile, request.getRoleIds());

        seedAccountsIfPresent(request.getClientId(), request.getAccounts(), "backoffice");

        return BackofficeEmployeeOnboardingResponse.builder()
                .iamUserId(iamUser.getId())
                .username(username)
                .generatedPassword(rawPassword)
                .build();
    }

    public void validateOrganization(BackofficeOrganizationValidationRequest request) {
        if (partyRepository.existsByCoreCustomerId(request.getClientId())) {
            throw BaseException.badRequest("Client ID '" + request.getClientId() + "' is already registered.");
        }

        if (request.getRegistrationNo() != null && !request.getRegistrationNo().isBlank()) {
            if (organizationRepository.existsByRegistrationNo(request.getRegistrationNo())) {
                throw BaseException.badRequest("Registration number '" + request.getRegistrationNo() + "' is already registered.");
            }
        }
    }

    @Transactional
    public BackofficeOrganizationOnboardingResponse createOrganization(BackofficeOrganizationOnboardingRequest request) {
        validateOrganization(BackofficeOrganizationValidationRequest.builder()
                .clientId(request.getClientId())
                .registrationNo(request.getRegistrationNo())
                .build());

        ensureClientExists(request.getClientId());

        PartyEntity party = createParty(PartyType.ORGANIZATION, request.getClientId());
        OrganizationEntity organization = new OrganizationEntity();
        organization.setParty(party);
        organization.setLegalName(request.getLegalName());
        organization.setDisplayName(request.getDisplayName());
        organization.setRegistrationNo(request.getRegistrationNo());
        organization.setCustomerSegment(request.getCustomerSegment());
        organization.setSmeMode(request.getSmeMode());
        organization.setCountryCode(resolveCountry(request.getCountry()));
        organization.setAddress(request.getAddress());
        organization.setCity(request.getCity());
        organization.setCompanyPhone(request.getCompanyPhone());
        organization.setCompanyEmail(request.getCompanyEmail());
        organization.setContactPersonName(request.getContactPersonName());
        organization.setContactPersonEmail(request.getContactPersonEmail());
        organization.setContactPersonPhone(request.getContactPersonPhone());
        organization.setCreatedAt(OffsetDateTime.now());
        organization.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(organization);

        maybeCreateOrganizationUser(party, request.getOrgUser());

        seedAccountsIfPresent(request.getClientId(), request.getAccounts(), "backoffice");

        return BackofficeOrganizationOnboardingResponse.builder()
                .partyId(party.getId())
                .publicId(party.getPublicId())
                .legalName(organization.getLegalName())
                .build();
    }

    private void maybeCreateOrganizationUser(PartyEntity organizationParty, BackofficeOrganizationUserRequest request) {
        if (request == null) {
            return;
        }
        if (request.getOrgRoleId() == null) {
            log.info("Skipping org user creation because orgRoleId was not provided.");
            return;
        }

        OrgRoleEntity role = orgRoleRepository.findById(request.getOrgRoleId())
                .orElseThrow(() -> BaseException.notFound("Organization role not found"));

        if (!Objects.equals(role.getOrganizationParty().getId(), organizationParty.getId())) {
            throw BaseException.badRequest("Organization role does not belong to this organization.");
        }

        PartyEntity party = createParty(PartyType.PERSON, null);
        createPerson(party, request.getFirstName(), request.getMiddleName(), request.getLastName(),
                request.getNationalId(), null, null, null, null, null);
        IamUserEntity iamUser = createIamUser(party);

        UserContact phone = createUserContact(iamUser, ContactType.PHONE, request.getMobile());
        UserContact email = createUserContact(iamUser, ContactType.EMAIL, request.getEmail());

        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = generateUniqueUsername(Channel.INTERNET_BANKING);
        }
        createLoginIdentifier(iamUser, username, Channel.INTERNET_BANKING);

        OrganizationUserEntity organizationUser = new OrganizationUserEntity();
        organizationUser.setIamUser(iamUser);
        organizationUser.setOrganizationParty(organizationParty);
        organizationUser.setOrgRole(role);
        organizationUser.setIsPrimary(Boolean.TRUE.equals(request.getPrimary()));
        organizationUser.setStatus("ACTIVE");
        organizationUserRepository.save(organizationUser);
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

    private void seedAccountsIfPresent(String clientId, List<BackofficeAccountRequest> accounts, String createdBy) {
        if (accounts == null || accounts.isEmpty()) {
            return;
        }

        Long clientIdLong;
        try {
            clientIdLong = Long.parseLong(clientId);
        } catch (NumberFormatException ex) {
            throw BaseException.badRequest("Client ID must be numeric for account seeding.");
        }

        List<BackofficeAccountSeedItem> seedItems = accounts.stream()
                .filter(Objects::nonNull)
                .map(account -> BackofficeAccountSeedItem.builder()
                        .accountNumber(account.getAccountNumber())
                        .accountName(account.getAccountName())
                        .currency(account.getCurrency())
                        .iban(account.getIban())
                        .branchId(account.getBranchId())
                        .branchName(account.getBranchName())
                        .phone(account.getPhone())
                        .email(account.getEmail())
                        .productId(account.getProductId())
                        .productName(account.getProductName())
                        .allowCredit(account.getAllowCredit())
                        .allowDebit(account.getAllowDebit())
                        .allowWaafi(account.getAllowWaafi())
                        .primary(account.getPrimary())
                        .build())
                .toList();

        if (seedItems.isEmpty()) {
            return;
        }

        BackofficeAccountSeedRequest seedRequest = BackofficeAccountSeedRequest.builder()
                .clientId(clientIdLong)
                .accounts(seedItems)
                .createdBy(createdBy)
                .build();

        accountBackofficeClient.seedClientAccounts(seedRequest);
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

    private String generateUniqueUsername(Channel channel) {
        return UsernameGeneratorUtil.generateUniqueNumericUsername(8,
                username -> loginIdentifierRepository.existsByChannelAndIdentifierTypeAndIdentifier(
                        channel, "username", username));
    }

    private String generatePassword(Channel channel) {
        int length = 8;
        var policy = passwordPolicyService.resolvePolicy(channel);
        if (policy != null && policy.getMinLength() != null && policy.getMinLength() > 0) {
            length = policy.getMinLength();
        }

        String password;
        if (channel == Channel.INTERNET_BANKING) {
            password = PasswordGeneratorUtil.generateNumericPassword(length);
        } else {
            int attempts = 0;
            while (true) {
                password = PasswordGeneratorUtil.generateRandomPassword(length);
                if (policy == null) {
                    break;
                }
                try {
                    passwordPolicyService.validateStructure(password, policy);
                    break;
                } catch (Exception ex) {
                    attempts++;
                    if (attempts > 10) {
                        break;
                    }
                }
            }
        }
        return password;
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

    private BackofficeCustomerDetailsResponse ensureClientExists(String clientId) {
        BackofficeCustomerDetailsResponse response = accountBackofficeClient.getClientDetails(clientId)
                .orElseThrow(() -> BaseException.notFound("Client ID '" + clientId + "' not found in core banking."));
        if (response.getClientId() == null || response.getClientId().isBlank()) {
            throw BaseException.notFound("Client ID '" + clientId + "' not found in core banking.");
        }
        return response;
    }

    private BackofficeCustomerDetailsResponse ensureIndividualClientExists(String clientId) {
        BackofficeCustomerDetailsResponse response = ensureClientExists(clientId);
        if (response.getClientTypeId() == null || !"I".equalsIgnoreCase(response.getClientTypeId())) {
            throw BaseException.badRequest("Only individual customers (ClientTypeID=I) are allowed.");
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
                    if (account.getAccountName() == null || account.getAccountName().isBlank()) {
                        throw BaseException.badRequest("Account name missing for '" + num + "'.");
                    }
                    //TODO check currency if it will be added, otherwise USD
//                    if (account.getCurrency() == null || account.getCurrency().isBlank()) {
//                        throw BaseException.badRequest("Account currency missing for '" + num + "'.");
//                    }
                    return BackofficeAccountSeedItem.builder()
                            .accountNumber(account.getAccountNumber())
                            .accountName(account.getAccountName())
                            .currency(account.getCurrency())
                            .iban(account.getIban())
                            .branchId(account.getBranchId())
                            .branchName(account.getBranchName())
                            .phone(account.getMobile())
                            .email(account.getEmail())
                            .productId(account.getProductId())
                            .productName(account.getProductName())
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
