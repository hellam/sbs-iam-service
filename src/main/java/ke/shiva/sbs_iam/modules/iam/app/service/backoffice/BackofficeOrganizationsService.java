package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.client.account.dto.response.BackofficeCustomerDetailsResponse;
import ke.shiva.client.iam.enums.TaskRole;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationRoleCreateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserAddRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserBasicKycUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserOnboardNonBankRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserRoleUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserSearchRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationPermissionResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationRoleDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationRoleResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationRolesPermissionsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserOnboardResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserSearchItemResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserSearchResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordUpdateService;
import ke.shiva.sbs_iam.modules.iam.app.service.SessionRevocationService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionIdEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.external.NotificationService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeProfileRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.FeatureRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrgRoleRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrgRolePermissionRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PartyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PersonRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.PaginationUtil;
import ke.shiva.shivacorestarter.util.PasswordGeneratorUtil;
import ke.shiva.shivacorestarter.util.UsernameGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeOrganizationsService {

    private final OrganizationRepository organizationRepository;
    private final PartyRepository partyRepository;
    private final PersonRepository personRepository;
    private final IamUserRepository iamUserRepository;
    private final LoginIdentifierRepository loginIdentifierRepository;
    private final OrgRoleRepository orgRoleRepository;
    private final OrgRolePermissionRepository orgRolePermissionRepository;
    private final FeatureRepository featureRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserContactRepository userContactRepository;
    private final CountryRepository countryRepository;
    private final PasswordUpdateService passwordUpdateService;
    private final SessionRevocationService sessionRevocationService;
    private final NotificationService notificationService;
    private final BackofficeOnboardingService onboardingService;
    @Autowired
    @Qualifier("accountRestClient")
    private RestClient accountRestClient;

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
                "accountLocked",
                "party.coreCustomerId",
                "party.status",
                "countryCode.countryName"
        );
        List<String> filterableColumns = List.of(
                "customerSegment",
                "smeMode",
                "accountLocked",
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

    @Transactional(readOnly = true)
    public List<BackofficeOrganizationUserResponse> getOrganizationUsers(String clientId) {
        String normalizedClientId = normalizeClientId(clientId);
        return organizationUserRepository.findByOrganizationParty_CoreCustomerIdOrderByCreatedAtDesc(normalizedClientId)
                .stream()
                .map(this::toOrganizationUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BackofficeOrganizationRoleResponse> getOrganizationRoles(String clientId) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        PartyEntity organizationParty = requireOrganizationParty(organization);
        return sortOrganizationRoles(orgRoleRepository.findAllByOrganizationParty(organizationParty)).stream()
                .map(this::toOrganizationRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BackofficeOrganizationRolesPermissionsResponse getOrganizationRolesPermissions(String clientId) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        PartyEntity organizationParty = requireOrganizationParty(organization);

        List<OrgRoleEntity> roles = sortOrganizationRoles(orgRoleRepository.findAllByOrganizationParty(organizationParty));
        List<FeatureEntity> availableFeatures = sortFeatures(
                featureRepository.findByEnabledTrueAndChannelOrderByCategoryAscNameAsc(Channel.INTERNET_BANKING)
        );

        Map<Long, List<FeatureEntity>> featuresByRoleId = new LinkedHashMap<>();
        for (OrgRolePermissionEntity rolePermission : orgRolePermissionRepository.findByOrgRole_OrganizationParty(organizationParty)) {
            if (rolePermission.getOrgRole() == null || rolePermission.getOrgRole().getId() == null) {
                continue;
            }
            if (rolePermission.getFeature() == null) {
                continue;
            }

            featuresByRoleId
                    .computeIfAbsent(rolePermission.getOrgRole().getId(), ignored -> new java.util.ArrayList<>())
                    .add(rolePermission.getFeature());
        }

        List<BackofficeOrganizationRoleDetailsResponse> roleResponses = roles.stream()
                .map(role -> toOrganizationRoleDetailsResponse(role, sortFeatures(featuresByRoleId.get(role.getId()))))
                .toList();

        List<BackofficeOrganizationPermissionResponse> availablePermissions = availableFeatures.stream()
                .map(this::toOrganizationPermissionResponse)
                .toList();

        return BackofficeOrganizationRolesPermissionsResponse.builder()
                .roles(roleResponses)
                .availablePermissions(availablePermissions)
                .build();
    }

    @Transactional
    public BackofficeOrganizationRoleDetailsResponse createOrganizationRole(
            String clientId,
            BackofficeOrganizationRoleCreateRequest request
    ) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        PartyEntity organizationParty = requireOrganizationParty(organization);

        String roleName = trimToNull(request.getName());
        if (!StringUtils.hasText(roleName)) {
            throw BaseException.badRequest("Role name is required.");
        }

        ensureRoleNameUnique(organizationParty, roleName, null);
        List<FeatureEntity> selectedFeatures = resolveSelectedOrganizationRolePermissions(request.getFeatureIds());

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (isDefault) {
            clearDefaultOrganizationRole(organizationParty, null);
        }

        OrgRoleEntity role = new OrgRoleEntity();
        role.setOrganizationParty(organizationParty);
        role.setName(roleName);
        role.setDescription(trimToNull(request.getDescription()));
        role.setTaskRole(request.getTaskRole());
        role.setIsDefault(isDefault);
        role.setIsActive(true);
        role.setCreatedAt(OffsetDateTime.now());
        role.setUpdatedAt(OffsetDateTime.now());
        role = orgRoleRepository.save(role);

        replaceRolePermissions(role, selectedFeatures);
        return toOrganizationRoleDetailsResponse(role, sortFeatures(selectedFeatures));
    }

    @Transactional
    public BackofficeOrganizationRoleDetailsResponse updateOrganizationRole(
            String clientId,
            Long roleId,
            BackofficeOrganizationRoleCreateRequest request
    ) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        PartyEntity organizationParty = requireOrganizationParty(organization);
        OrgRoleEntity role = resolveOrganizationRoleForUpdate(organizationParty, roleId);

        String roleName = trimToNull(request.getName());
        if (!StringUtils.hasText(roleName)) {
            throw BaseException.badRequest("Role name is required.");
        }

        ensureRoleNameUnique(organizationParty, roleName, role.getId());
        List<FeatureEntity> selectedFeatures = resolveSelectedOrganizationRolePermissions(request.getFeatureIds());

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (isDefault) {
            clearDefaultOrganizationRole(organizationParty, role.getId());
        }

        role.setName(roleName);
        role.setDescription(trimToNull(request.getDescription()));
        role.setTaskRole(request.getTaskRole());
        role.setIsDefault(isDefault);
        role.setUpdatedAt(OffsetDateTime.now());
        role = orgRoleRepository.save(role);

        replaceRolePermissions(role, selectedFeatures);
        return toOrganizationRoleDetailsResponse(role, sortFeatures(selectedFeatures));
    }

    @Transactional(readOnly = true)
    public BackofficeOrganizationUserSearchResponse searchOrganizationUsers(
            String clientId,
            BackofficeOrganizationUserSearchRequest request
    ) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        PartyEntity organizationParty = requireOrganizationParty(organization);
        SearchCriteria criteria = normalizeSearchCriteria(request);

        if (!criteria.hasAny()) {
            throw BaseException.badRequest(
                    "Provide at least one search field: customerId, phone, idNumber, email, or passport."
            );
        }

        List<IamUserEntity> candidates = findSearchCandidates(criteria).stream()
                .filter(this::isInternetEligibleUser)
                .sorted((first, second) -> {
                    String firstName = String.valueOf(first.getParty() != null && first.getParty().getPerson() != null
                            ? first.getParty().getPerson().getFullName()
                            : "").toUpperCase();
                    String secondName = String.valueOf(second.getParty() != null && second.getParty().getPerson() != null
                            ? second.getParty().getPerson().getFullName()
                            : "").toUpperCase();
                    int compare = firstName.compareTo(secondName);
                    if (compare != 0) {
                        return compare;
                    }
                    return Long.compare(
                            first.getId() != null ? first.getId() : Long.MAX_VALUE,
                            second.getId() != null ? second.getId() : Long.MAX_VALUE
                    );
                })
                .limit(10)
                .toList();

        boolean coreIndividualFound = false;
        String coreIndividualClientId = null;
        if (candidates.isEmpty() && criteria.customerId() != null && isCoreIndividualCustomer(criteria.customerId())) {
            coreIndividualFound = true;
            coreIndividualClientId = criteria.customerId();
        }

        List<BackofficeOrganizationUserSearchItemResponse> items = candidates.stream()
                .map(iamUser -> toSearchItemResponse(iamUser, organizationParty))
                .toList();

        return BackofficeOrganizationUserSearchResponse.builder()
                .candidates(items)
                .coreIndividualFound(coreIndividualFound)
                .coreIndividualClientId(coreIndividualClientId)
                .build();
    }

    @Transactional
    public BackofficeOrganizationUserResponse addOrganizationUser(
            String clientId,
            BackofficeOrganizationUserAddRequest request
    ) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        PartyEntity organizationParty = requireOrganizationParty(organization);

        Long iamUserId = request.getIamUserId();
        if (iamUserId == null || iamUserId <= 0) {
            throw BaseException.badRequest("iamUserId is required.");
        }

        IamUserEntity iamUser = iamUserRepository.findById(iamUserId)
                .orElseThrow(() -> BaseException.notFound("IAM user not found."));

        if (iamUser.getStatus() != IamStatus.ACTIVE) {
            throw BaseException.badRequest("Only active users can be linked to a company.");
        }

        ensureInternetCredentials(iamUser);

        if (organizationUserRepository.findByIamUserAndOrganizationParty(iamUser, organizationParty).isPresent()) {
            throw BaseException.badRequest("User is already linked to this company.");
        }

        OrgRoleEntity orgRole = resolveOrganizationRole(organizationParty, request.getOrgRoleId());
        OrganizationUserEntity organizationUser = createOrganizationUserLink(iamUser, organizationParty, orgRole);
        organizationUserRepository.save(organizationUser);

        linkAccountsToOrganizationUser(iamUser.getId(), organizationParty.getCoreCustomerId(), request.getClientAccountIds());
        sendOrganizationUserLinkedNotification(iamUser, organization, organizationParty);
        return toOrganizationUserResponse(organizationUser);
    }

    @Transactional
    public BackofficeOrganizationUserOnboardResponse onboardNonBankOrganizationUser(
            String clientId,
            BackofficeOrganizationUserOnboardNonBankRequest request
    ) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        PartyEntity organizationParty = requireOrganizationParty(organization);

        String customerId = trimToNull(request.getCustomerId());
        if (customerId != null) {
            if (partyRepository.existsByCoreCustomerId(customerId)) {
                throw BaseException.badRequest("Customer ID '" + customerId + "' is already registered.");
            }
            if (isCoreIndividualCustomer(customerId)) {
                throw BaseException.badRequest(
                        "Client ID '" + customerId + "' exists in core banking. Onboard from Individuals page."
                );
            }
        }

        String[] names = resolvePersonNames(request.getFullName());
        String email = trimToNull(request.getEmail());
        if (email != null) {
            email = email.toLowerCase();
        }
        String phone = trimToNull(request.getPhone());
        String nationalIdentifier = normalizeIdentityValue(firstNonBlank(request.getIdNumber(), request.getPassport()));
        if (!StringUtils.hasText(nationalIdentifier)) {
            throw BaseException.badRequest("Provide either idNumber or passport.");
        }

        validateNonBankUserUniqueness(email, phone, nationalIdentifier);

        PartyEntity party = createParty(PartyType.PERSON, customerId);
        createPerson(
                party,
                names[0],
                names[1],
                names[2],
                nationalIdentifier
        );
        IamUserEntity iamUser = createIamUser(party);

        if (phone != null) {
            createUserContact(iamUser, ContactType.PHONE, phone);
        }
        if (email != null) {
            createUserContact(iamUser, ContactType.EMAIL, email);
        }

        String username = generateUniqueInternetUsername();
        createInternetLoginIdentifier(iamUser, username);

        String rawPassword = PasswordGeneratorUtil.generateRandomPassword(16);
        createCustomerAuth(iamUser, rawPassword);

        OrgRoleEntity orgRole = resolveOrganizationRole(organizationParty, request.getOrgRoleId());
        OrganizationUserEntity organizationUser = createOrganizationUserLink(iamUser, organizationParty, orgRole);
        organizationUserRepository.save(organizationUser);

        linkAccountsToOrganizationUser(iamUser.getId(), organizationParty.getCoreCustomerId(), request.getClientAccountIds());
        sendOrganizationUserOnboardedNotification(
                iamUser,
                organization,
                organizationParty,
                username,
                rawPassword
        );

        return BackofficeOrganizationUserOnboardResponse.builder()
                .organizationUser(toOrganizationUserResponse(organizationUser))
                .build();
    }

    @Transactional
    public BackofficeOrganizationSummaryResponse updateOrganizationAccountLock(String clientId, boolean blocked) {
        String normalizedClientId = normalizeClientId(clientId);
        OrganizationEntity organization = getRequiredOrganization(clientId);
        organization.setAccountLocked(blocked);
        organization.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(organization);

        if (blocked) {
            Set<Long> handledUsers = new HashSet<>();
            for (OrganizationUserEntity organizationUser : organizationUserRepository
                    .findByOrganizationParty_CoreCustomerIdOrderByCreatedAtDesc(normalizedClientId)) {
                IamUserEntity iamUser = organizationUser.getIamUser();
                if (iamUser == null || iamUser.getId() == null || !handledUsers.add(iamUser.getId())) {
                    continue;
                }
                sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_ORGANIZATION_BLOCKED");
            }
        }

        return toResponse(organization);
    }

    @Transactional
    public BackofficeOrganizationSummaryResponse syncOrganizationKyc(String clientId) {
        OrganizationEntity organization = getRequiredOrganization(clientId);
        BackofficeCustomerDetailsResponse coreDetails = onboardingService.fetchOrganizationCoreDetails(clientId);

        String legalName = firstNonBlank(
                trimToNull(coreDetails.getFullName()),
                buildFullName(coreDetails.getFirstName(), coreDetails.getMiddleName(), coreDetails.getLastName()),
                organization.getLegalName()
        );
        if (!StringUtils.hasText(legalName)) {
            throw BaseException.badRequest("Legal name is required from core banking.");
        }

        organization.setLegalName(legalName);
        organization.setDisplayName(legalName);
        organization.setCompanyPhone(trimToNull(coreDetails.getMobile()));
        organization.setCompanyEmail(trimToNull(coreDetails.getEmail()));
        organization.setCity(trimToNull(coreDetails.getCity()));
        organization.setAddress(trimToNull(coreDetails.getAddress1()));
        organization.setCountryCode(resolveCountry(coreDetails.getCountryId(), coreDetails.getCountryName()));
        organization.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(organization);

        return toResponse(organization);
    }

    @Transactional(readOnly = true)
    public BackofficeOrganizationUserResponse getOrganizationUser(String clientId, Long organizationUserId) {
        return toOrganizationUserResponse(getRequiredOrganizationUser(clientId, organizationUserId));
    }

    @Transactional
    public BackofficeOrganizationUserResponse updateOrganizationUserAccessLock(String clientId, Long organizationUserId, boolean blocked) {
        OrganizationUserEntity organizationUser = getRequiredOrganizationUser(clientId, organizationUserId);
        IamUserEntity iamUser = requireOrganizationUserIamUser(organizationUser);
        CustomerAuthEntity auth = requireCustomerAuth(iamUser);

        auth.setInternetLocked(blocked);
        if (blocked) {
            auth.setInternetLockoutUntil(null);
        } else {
            auth.setInternetLockoutUntil(null);
            auth.setInternetFailedAttempts((short) 0);
        }
        customerAuthRepository.save(auth);

        if (blocked) {
            sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_ORGANIZATION_USER_BLOCKED");
        }

        return toOrganizationUserResponse(organizationUser);
    }

    @Transactional
    public BackofficeOrganizationUserResponse resetOrganizationUserPassword(String clientId, Long organizationUserId) {
        OrganizationUserEntity organizationUser = getRequiredOrganizationUser(clientId, organizationUserId);
        IamUserEntity iamUser = requireOrganizationUserIamUser(organizationUser);
        requireCustomerAuth(iamUser);

        String randomPassword = PasswordGeneratorUtil.generateRandomPassword(16);
        passwordUpdateService.updatePassword(iamUser, randomPassword, Channel.INTERNET_BANKING, true);
        sessionRevocationService.revokeAllActiveSessionsForUser(iamUser, "BACKOFFICE_ORGANIZATION_USER_PASSWORD_RESET");
        sendPasswordResetNotification(iamUser, organizationUser, randomPassword);

        return toOrganizationUserResponse(organizationUser);
    }

    @Transactional
    public BackofficeOrganizationUserResponse resetOrganizationUserMfa(String clientId, Long organizationUserId) {
        OrganizationUserEntity organizationUser = getRequiredOrganizationUser(clientId, organizationUserId);
        IamUserEntity iamUser = requireOrganizationUserIamUser(organizationUser);
        CustomerAuthEntity auth = requireCustomerAuth(iamUser);
        if (!isMfaTotpConfigured(auth)) {
            throw BaseException.badRequest("MFA TOTP is not configured for this user.");
        }

        auth.setMfaEnabled(false);
        auth.setMfaSecret(null);
        auth.setMfaLastVerifiedAt(null);
        customerAuthRepository.save(auth);

        return toOrganizationUserResponse(organizationUser);
    }

    @Transactional
    public BackofficeOrganizationUserResponse syncOrganizationUserKyc(String clientId, Long organizationUserId) {
        OrganizationUserEntity organizationUser = getRequiredOrganizationUser(clientId, organizationUserId);
        IamUserEntity iamUser = requireOrganizationUserIamUser(organizationUser);
        if (!isVerifiedIndividual(iamUser)) {
            throw BaseException.badRequest("User is unverified. Use basic KYC update.");
        }

        PartyEntity party = iamUser.getParty();
        PersonEntity person = party != null ? party.getPerson() : null;
        if (party == null || person == null || !StringUtils.hasText(party.getCoreCustomerId())) {
            throw BaseException.badRequest("User profile is incomplete for KYC sync.");
        }

        BackofficeCustomerDetailsResponse coreDetails = onboardingService.fetchCustomerCoreDetails(party.getCoreCustomerId());
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

        return toOrganizationUserResponse(organizationUser);
    }

    @Transactional
    public BackofficeOrganizationUserResponse updateOrganizationUserBasicKyc(
            String clientId,
            Long organizationUserId,
            BackofficeOrganizationUserBasicKycUpdateRequest request
    ) {
        OrganizationUserEntity organizationUser = getRequiredOrganizationUser(clientId, organizationUserId);
        IamUserEntity iamUser = requireOrganizationUserIamUser(organizationUser);
        if (isVerifiedIndividual(iamUser)) {
            throw BaseException.badRequest("Verified users must be synced from core banking.");
        }

        PartyEntity party = iamUser.getParty();
        PersonEntity person = party != null ? party.getPerson() : null;
        if (party == null || person == null) {
            throw BaseException.badRequest("User profile is incomplete.");
        }

        String[] names = resolvePersonNames(request.getFullName());
        person.setFirstName(names[0]);
        person.setLastName(names[2]);
        person.setFullName(buildFullName(names[0], names[1], names[2]));
        person.setUpdatedAt(OffsetDateTime.now());

        upsertPrimaryContact(iamUser, ContactType.EMAIL, trimToNull(request.getEmail()));
        upsertPrimaryContact(iamUser, ContactType.PHONE, trimToNull(request.getPhone()));

        return toOrganizationUserResponse(organizationUser);
    }

    @Transactional
    public BackofficeOrganizationUserResponse updateOrganizationUserRole(
            String clientId,
            Long organizationUserId,
            BackofficeOrganizationUserRoleUpdateRequest request
    ) {
        OrganizationUserEntity organizationUser = getRequiredOrganizationUser(clientId, organizationUserId);
        PartyEntity organizationParty = organizationUser.getOrganizationParty();
        OrgRoleEntity orgRole = resolveOrganizationRole(organizationParty, request.getOrgRoleId());

        organizationUser.setOrgRole(orgRole);
        organizationUser.setUpdatedAt(OffsetDateTime.now());
        organizationUserRepository.save(organizationUser);

        return toOrganizationUserResponse(organizationUser);
    }

    private SearchCriteria normalizeSearchCriteria(BackofficeOrganizationUserSearchRequest request) {
        if (request == null) {
            return new SearchCriteria(null, null, null, null, null);
        }

        String customerId = trimToNull(request.getCustomerId());
        String phone = normalizePhoneSearchValue(request.getPhone());
        String idNumber = normalizeIdentityValue(request.getIdNumber());
        String email = trimToNull(request.getEmail());
        if (email != null) {
            email = email.toLowerCase();
        }
        String passport = normalizeIdentityValue(request.getPassport());
        return new SearchCriteria(customerId, phone, idNumber, email, passport);
    }

    private List<IamUserEntity> findSearchCandidates(SearchCriteria criteria) {
        Map<Long, IamUserEntity> merged = new LinkedHashMap<>();

        if (criteria.customerId() != null) {
            addSearchCandidates(merged, loadUsersByCustomerId(criteria.customerId()));
        }
        if (criteria.phone() != null) {
            addSearchCandidates(merged, loadUsersByPhone(criteria.phone()));
        }
        if (criteria.idNumber() != null) {
            addSearchCandidates(merged, loadUsersByNationalId(criteria.idNumber()));
        }
        if (criteria.email() != null) {
            addSearchCandidates(merged, loadUsersByEmail(criteria.email()));
        }
        if (criteria.passport() != null) {
            addSearchCandidates(merged, loadUsersByNationalId(criteria.passport()));
        }

        return merged.values().stream()
                .filter(candidate -> candidate != null && candidate.getId() != null)
                .toList();
    }

    private void addSearchCandidates(
            Map<Long, IamUserEntity> merged,
            List<IamUserEntity> candidates
    ) {
        for (IamUserEntity candidate : candidates) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            merged.putIfAbsent(candidate.getId(), candidate);
        }
    }

    private List<IamUserEntity> loadUsersByCustomerId(String customerId) {
        if (!StringUtils.hasText(customerId)) {
            return List.of();
        }

        Map<Long, IamUserEntity> matches = new LinkedHashMap<>();
        addSearchCandidate(matches, iamUserRepository.findFirstByParty_CoreCustomerIdIgnoreCase(customerId).orElse(null));
        addSearchCandidate(matches, customerProfileRepository.findByCoreCustomerIdIgnoreCase(customerId)
                .map(profile -> profile.getIamUser())
                .orElse(null));
        addSearchCandidate(matches, employeeProfileRepository.findFirstByIamUser_Party_CoreCustomerIdIgnoreCase(customerId)
                .map(profile -> profile.getIamUser())
                .orElse(null));

        return matches.values().stream()
                .filter(candidate -> candidate != null && candidate.getId() != null)
                .toList();
    }

    private void addSearchCandidate(Map<Long, IamUserEntity> merged, IamUserEntity candidate) {
        if (candidate == null || candidate.getId() == null) {
            return;
        }
        merged.putIfAbsent(candidate.getId(), candidate);
    }

    private List<IamUserEntity> loadUsersByNationalId(String nationalId) {
        return iamUserRepository.findFirstByParty_Person_NationalIdIgnoreCase(nationalId)
                .map(List::of)
                .orElse(List.of());
    }

    private List<IamUserEntity> loadUsersByEmail(String email) {
        return userContactRepository.findFirstByContactTypeAndContactValueIgnoreCaseAndPrimaryIsTrue(ContactType.EMAIL, email)
                .map(UserContact::getIamUser)
                .map(List::of)
                .orElse(List.of());
    }

    private List<IamUserEntity> loadUsersByPhone(String phoneFragment) {
        return userContactRepository.findByContactTypeAndContactValueContainingIgnoreCaseAndPrimaryIsTrue(
                        ContactType.PHONE,
                        phoneFragment
                )
                .stream()
                .map(UserContact::getIamUser)
                .filter(iamUser -> iamUser != null && iamUser.getId() != null)
                .toList();
    }

    private boolean isInternetEligibleUser(IamUserEntity iamUser) {
        if (iamUser == null || iamUser.getId() == null || iamUser.getStatus() != IamStatus.ACTIVE) {
            return false;
        }
        if (iamUser.getCustomerProfile() == null && iamUser.getEmployeeProfile() == null) {
            return false;
        }
        if (customerAuthRepository.findByIamUserId(iamUser.getId()).isEmpty()) {
            return false;
        }
        return loginIdentifierRepository.findByIamUserAndChannelAndIdentifierType(
                iamUser,
                Channel.INTERNET_BANKING,
                "username"
        ).isPresent();
    }

    private BackofficeOrganizationUserSearchItemResponse toSearchItemResponse(
            IamUserEntity iamUser,
            PartyEntity organizationParty
    ) {
        PartyEntity party = iamUser.getParty();
        PersonEntity person = party != null ? party.getPerson() : null;
        CustomerAuthEntity auth = customerAuthRepository.findByIamUserId(iamUser.getId()).orElse(null);
        boolean alreadyLinked = organizationUserRepository.findByIamUserAndOrganizationParty(iamUser, organizationParty)
                .isPresent();

        return BackofficeOrganizationUserSearchItemResponse.builder()
                .iamUserId(iamUser.getId())
                .individualClientId(party != null ? party.getCoreCustomerId() : null)
                .fullName(person != null ? person.getFullName() : null)
                .mobile(resolvePrimaryContact(iamUser, ContactType.PHONE))
                .email(resolvePrimaryContact(iamUser, ContactType.EMAIL))
                .verified(isVerifiedIndividual(iamUser))
                .internetLocked(auth != null && Boolean.TRUE.equals(auth.getInternetLocked()))
                .mfaTotpEnabled(isMfaTotpConfigured(auth))
                .alreadyLinked(alreadyLinked)
                .build();
    }

    private PartyEntity requireOrganizationParty(OrganizationEntity organization) {
        if (organization == null || organization.getId() == null) {
            throw BaseException.notFound("Organization not found.");
        }
        return partyRepository.findById(organization.getId())
                .orElseThrow(() -> BaseException.notFound("Organization party not found."));
    }

    private OrgRoleEntity resolveOrganizationRole(PartyEntity organizationParty, Long orgRoleId) {
        if (orgRoleId == null || orgRoleId <= 0) {
            throw BaseException.badRequest("orgRoleId is required.");
        }

        OrgRoleEntity role = orgRoleRepository.findById(orgRoleId)
                .orElseThrow(() -> BaseException.notFound("Organization role not found."));

        Long organizationPartyId = organizationParty != null ? organizationParty.getId() : null;
        Long roleOrganizationPartyId = role.getOrganizationParty() != null ? role.getOrganizationParty().getId() : null;
        if (!Objects.equals(organizationPartyId, roleOrganizationPartyId)) {
            throw BaseException.badRequest("Selected role does not belong to this company.");
        }

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw BaseException.badRequest("Selected role is inactive.");
        }
        return role;
    }

    private OrgRoleEntity resolveOrganizationRoleForUpdate(PartyEntity organizationParty, Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw BaseException.badRequest("roleId is required.");
        }

        OrgRoleEntity role = orgRoleRepository.findById(roleId)
                .orElseThrow(() -> BaseException.notFound("Organization role not found."));

        Long organizationPartyId = organizationParty != null ? organizationParty.getId() : null;
        Long roleOrganizationPartyId = role.getOrganizationParty() != null ? role.getOrganizationParty().getId() : null;
        if (!Objects.equals(organizationPartyId, roleOrganizationPartyId)) {
            throw BaseException.badRequest("Selected role does not belong to this company.");
        }

        return role;
    }

    private void ensureRoleNameUnique(PartyEntity organizationParty, String roleName, Long currentRoleId) {
        orgRoleRepository.findByOrganizationPartyAndNameIgnoreCase(organizationParty, roleName)
                .ifPresent(existingRole -> {
                    Long existingRoleId = existingRole.getId();
                    if (currentRoleId == null || !Objects.equals(existingRoleId, currentRoleId)) {
                        throw BaseException.badRequest("Role name '" + roleName + "' already exists for this company.");
                    }
                });
    }

    private List<FeatureEntity> resolveSelectedOrganizationRolePermissions(List<Long> featureIdsInput) {
        List<Long> featureIds = featureIdsInput == null
                ? List.of()
                : featureIdsInput.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (featureIds.isEmpty()) {
            throw BaseException.badRequest("Select at least one permission.");
        }

        List<FeatureEntity> selectedFeatures = featureRepository.findAllById(featureIds);
        if (selectedFeatures.size() != featureIds.size()) {
            throw BaseException.badRequest("One or more selected permissions were not found.");
        }

        for (FeatureEntity feature : selectedFeatures) {
            if (feature.getChannel() != Channel.INTERNET_BANKING || !Boolean.TRUE.equals(feature.getEnabled())) {
                throw BaseException.badRequest("Permission '" + feature.getCode() + "' is not available for organization roles.");
            }
        }

        return selectedFeatures;
    }

    private void clearDefaultOrganizationRole(PartyEntity organizationParty, Long excludedRoleId) {
        List<OrgRoleEntity> existingRoles = orgRoleRepository.findAllByOrganizationParty(organizationParty);
        List<OrgRoleEntity> updated = existingRoles.stream()
                .filter(existingRole -> existingRole != null && existingRole.getId() != null)
                .filter(existingRole -> !Objects.equals(existingRole.getId(), excludedRoleId))
                .filter(existingRole -> Boolean.TRUE.equals(existingRole.getIsDefault()))
                .toList();

        if (updated.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (OrgRoleEntity existingRole : updated) {
            existingRole.setIsDefault(false);
            existingRole.setUpdatedAt(now);
        }
        orgRoleRepository.saveAll(updated);
    }

    private void replaceRolePermissions(OrgRoleEntity role, List<FeatureEntity> selectedFeatures) {
        List<OrgRolePermissionEntity> existingPermissions = orgRolePermissionRepository.findByOrgRole(role);
        if (!existingPermissions.isEmpty()) {
            orgRolePermissionRepository.deleteAll(existingPermissions);
        }

        List<OrgRolePermissionEntity> newPermissions = selectedFeatures.stream()
                .map(feature -> {
                    OrgRolePermissionIdEntity id = new OrgRolePermissionIdEntity();
                    id.setOrgRoleId(role.getId());
                    id.setFeatureId(feature.getId());

                    OrgRolePermissionEntity rolePermission = new OrgRolePermissionEntity();
                    rolePermission.setId(id);
                    rolePermission.setOrgRole(role);
                    rolePermission.setFeature(feature);
                    return rolePermission;
                })
                .toList();
        orgRolePermissionRepository.saveAll(newPermissions);
    }

    private OrganizationUserEntity createOrganizationUserLink(
            IamUserEntity iamUser,
            PartyEntity organizationParty,
            OrgRoleEntity orgRole
    ) {
        OrganizationUserEntity organizationUser = new OrganizationUserEntity();
        organizationUser.setIamUser(iamUser);
        organizationUser.setOrganizationParty(organizationParty);
        organizationUser.setOrgRole(orgRole);
        organizationUser.setIsPrimary(false);
        organizationUser.setStatus("ACTIVE");
        organizationUser.setCreatedAt(OffsetDateTime.now());
        organizationUser.setUpdatedAt(OffsetDateTime.now());
        return organizationUser;
    }

    private void ensureInternetCredentials(IamUserEntity iamUser) {
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.badRequest("IAM user is required.");
        }
        CustomerAuthEntity auth = customerAuthRepository.findByIamUserId(iamUser.getId())
                .orElseThrow(() -> BaseException.badRequest("User does not have internet banking credentials."));
        if (Boolean.TRUE.equals(auth.getInternetLocked())) {
            throw BaseException.badRequest("User is internet locked and cannot be added to a company.");
        }
        if (loginIdentifierRepository.findByIamUserAndChannelAndIdentifierType(
                iamUser,
                Channel.INTERNET_BANKING,
                "username"
        ).isEmpty()) {
            throw BaseException.badRequest("User does not have internet banking username.");
        }
    }

    private PartyEntity createParty(PartyType partyType, String coreCustomerId) {
        PartyEntity party = new PartyEntity();
        party.setPublicId(UUID.randomUUID());
        party.setPartyType(partyType);
        party.setStatus("ACTIVE");
        party.setCoreCustomerId(coreCustomerId);
        party.setCreatedAt(OffsetDateTime.now());
        party.setUpdatedAt(OffsetDateTime.now());
        return partyRepository.save(party);
    }

    private PersonEntity createPerson(
            PartyEntity party,
            String firstName,
            String middleName,
            String lastName,
            String nationalId
    ) {
        PersonEntity person = new PersonEntity();
        person.setParty(party);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setFullName(buildFullName(firstName, middleName, lastName));
        person.setNationalId(nationalId);
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

    private void createInternetLoginIdentifier(IamUserEntity iamUser, String username) {
        LoginIdentifierEntity loginIdentifier = new LoginIdentifierEntity();
        loginIdentifier.setIamUser(iamUser);
        loginIdentifier.setChannel(Channel.INTERNET_BANKING);
        loginIdentifier.setIdentifierType("username");
        loginIdentifier.setIdentifier(username);
        loginIdentifier.setStatus(IamStatus.ACTIVE);
        loginIdentifier.setCreatedAt(OffsetDateTime.now());
        loginIdentifier.setUpdatedAt(OffsetDateTime.now());
        loginIdentifierRepository.save(loginIdentifier);
    }

    private void createCustomerAuth(IamUserEntity iamUser, String rawPassword) {
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
        auth.setCreatedAt(OffsetDateTime.now());
        auth.setUpdatedAt(OffsetDateTime.now());
        customerAuthRepository.save(auth);
    }

    private void sendOrganizationUserLinkedNotification(
            IamUserEntity iamUser,
            OrganizationEntity organization,
            PartyEntity organizationParty
    ) {
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);
        if (!StringUtils.hasText(email)) {
            return;
        }

        String fullName = iamUser.getParty() != null && iamUser.getParty().getPerson() != null
                ? iamUser.getParty().getPerson().getFullName()
                : "Customer";
        String organizationName = firstNonBlank(
                trimToNull(organization.getDisplayName()),
                trimToNull(organization.getLegalName()),
                organizationParty != null ? trimToNull(organizationParty.getCoreCustomerId()) : null,
                "Company"
        );
        String organizationClientId = organizationParty != null
                ? trimToNull(organizationParty.getCoreCustomerId())
                : null;

        try {
            notificationService.sendOrganizationProfileLinkedNotice(
                    email,
                    fullName,
                    organizationName,
                    organizationClientId
            );
        } catch (Exception exception) {
            log.warn("Organization link notification failed for iamUserId={} orgClientId={}: {}",
                    iamUser != null ? iamUser.getId() : null,
                    organizationClientId,
                    exception.getMessage());
        }
    }

    private void sendOrganizationUserOnboardedNotification(
            IamUserEntity iamUser,
            OrganizationEntity organization,
            PartyEntity organizationParty,
            String username,
            String temporaryPassword
    ) {
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);
        if (!StringUtils.hasText(email)) {
            return;
        }

        String fullName = iamUser.getParty() != null && iamUser.getParty().getPerson() != null
                ? iamUser.getParty().getPerson().getFullName()
                : "Customer";
        String organizationName = firstNonBlank(
                trimToNull(organization.getDisplayName()),
                trimToNull(organization.getLegalName()),
                organizationParty != null ? trimToNull(organizationParty.getCoreCustomerId()) : null,
                "Company"
        );
        String organizationClientId = organizationParty != null
                ? trimToNull(organizationParty.getCoreCustomerId())
                : null;

        try {
            notificationService.sendOrganizationProfileOnboardedNotice(
                    email,
                    fullName,
                    organizationName,
                    organizationClientId,
                    username,
                    temporaryPassword
            );
        } catch (Exception exception) {
            log.warn("Organization onboarding notification failed for iamUserId={} orgClientId={}: {}",
                    iamUser != null ? iamUser.getId() : null,
                    organizationClientId,
                    exception.getMessage());
        }
    }

    private void validateNonBankUserUniqueness(String email, String phone, String nationalIdentifier) {
        if (email != null && userContactRepository.existsByContactTypeAndContactValueIgnoreCase(ContactType.EMAIL, email)) {
            throw BaseException.badRequest("Email '" + email + "' is already registered.");
        }

        if (phone != null) {
            String digits = phone.replaceAll("\\D+", "");
            String partial = digits.length() > 9 ? digits.substring(digits.length() - 9) : digits;
            if (StringUtils.hasText(partial)
                    && userContactRepository.existsByContactTypeAndContactValueContaining(ContactType.PHONE, partial)) {
                throw BaseException.badRequest("Phone number '" + phone + "' is already registered.");
            }
        }

        if (nationalIdentifier != null && personRepository.existsByNationalIdIgnoreCase(nationalIdentifier)) {
            throw BaseException.badRequest("ID/Passport '" + nationalIdentifier + "' is already registered.");
        }
    }

    private void linkAccountsToOrganizationUser(Long iamUserId, String clientId, List<String> clientAccountIds) {
        if (iamUserId == null || iamUserId <= 0) {
            throw BaseException.badRequest("Invalid IAM user ID.");
        }

        if (!StringUtils.hasText(clientId)) {
            throw BaseException.badRequest("Organization client ID is required for account linking.");
        }

        Long customerId;
        try {
            customerId = Long.parseLong(clientId.trim());
        } catch (NumberFormatException exception) {
            throw BaseException.badRequest("Organization client ID must be numeric for account linking.");
        }

        List<String> selectedAccountIds = sanitizeClientAccountIds(clientAccountIds);
        if (selectedAccountIds.isEmpty()) {
            throw BaseException.badRequest("Select at least one account to link.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iamUserId", iamUserId);
        payload.put("customerId", customerId);
        payload.put("clientAccountIds", selectedAccountIds);

        try {
            accountRestClient.post()
                    .uri("/internal/accounts/allocation/link")
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String body = readBody(response);
                        throw BaseException.badRequest(buildAccountLinkError(body));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        String body = readBody(response);
                        throw BaseException.badRequest(buildAccountLinkError(body));
                    })
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw BaseException.badRequest(buildAccountLinkError(exception.getResponseBodyAsString()));
        } catch (BaseException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Unable to link accounts for org user iamUserId={} customerId={}: {}",
                    iamUserId, customerId, exception.getMessage(), exception);
            throw BaseException.badRequest("Unable to link selected accounts at the moment.");
        }
    }

    private List<String> sanitizeClientAccountIds(List<String> clientAccountIds) {
        if (clientAccountIds == null) {
            return List.of();
        }

        return clientAccountIds.stream()
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String buildAccountLinkError(String responseBody) {
        String fallback = "Unable to link selected accounts.";
        String body = trimToNull(responseBody);
        if (body == null) {
            return fallback;
        }

        String normalized = body.replace("\n", " ").trim();
        if (normalized.length() > 220) {
            normalized = normalized.substring(0, 220);
        }
        return fallback + " " + normalized;
    }

    private String readBody(ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes());
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean isCoreIndividualCustomer(String customerId) {
        if (!StringUtils.hasText(customerId)) {
            return false;
        }

        try {
            onboardingService.fetchCustomerCoreDetails(customerId.trim());
            return true;
        } catch (BaseException exception) {
            return false;
        } catch (Exception exception) {
            log.warn("Core lookup failed for customerId={} during organization user add flow: {}",
                    customerId, exception.getMessage());
            return false;
        }
    }

    private String normalizeIdentityValue(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase();
    }

    private String normalizePhoneSearchValue(String phone) {
        String normalized = trimToNull(phone);
        if (normalized == null) {
            return null;
        }

        String digits = normalized.replaceAll("\\D+", "");
        if (!StringUtils.hasText(digits)) {
            return normalized;
        }
        return digits.length() > 9 ? digits.substring(digits.length() - 9) : digits;
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
                .accountLocked(Boolean.TRUE.equals(organization.getAccountLocked()))
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    private BackofficeOrganizationUserResponse toOrganizationUserResponse(OrganizationUserEntity entity) {
        IamUserEntity iamUser = entity.getIamUser();
        PartyEntity party = iamUser != null ? iamUser.getParty() : null;
        PersonEntity person = party != null ? party.getPerson() : null;

        String roleName = entity.getOrgRole() != null ? entity.getOrgRole().getName() : null;
        String taskRole = entity.getOrgRole() != null && entity.getOrgRole().getTaskRole() != null
                ? entity.getOrgRole().getTaskRole().name()
                : null;

        return BackofficeOrganizationUserResponse.builder()
                .organizationUserId(entity.getId())
                .iamUserId(iamUser != null ? iamUser.getId() : null)
                .individualClientId(party != null ? party.getCoreCustomerId() : null)
                .clientId(entity.getOrganizationParty() != null ? entity.getOrganizationParty().getCoreCustomerId() : null)
                .fullName(person != null ? person.getFullName() : null)
                .mobile(resolvePrimaryContact(iamUser, ContactType.PHONE))
                .email(resolvePrimaryContact(iamUser, ContactType.EMAIL))
                .verified(isVerifiedIndividual(iamUser))
                .internetLocked(iamUser != null
                        && iamUser.getCustomerAuth() != null
                        && Boolean.TRUE.equals(iamUser.getCustomerAuth().getInternetLocked()))
                .mfaTotpEnabled(isMfaTotpEnabled(iamUser))
                .orgRoleId(entity.getOrgRole() != null ? entity.getOrgRole().getId() : null)
                .roleName(roleName)
                .taskRole(taskRole)
                .primary(entity.getIsPrimary())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private BackofficeOrganizationRoleResponse toOrganizationRoleResponse(OrgRoleEntity entity) {
        return BackofficeOrganizationRoleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .taskRole(entity.getTaskRole() != null ? entity.getTaskRole().name() : null)
                .isDefault(Boolean.TRUE.equals(entity.getIsDefault()))
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private BackofficeOrganizationRoleDetailsResponse toOrganizationRoleDetailsResponse(
            OrgRoleEntity entity,
            List<FeatureEntity> permissions
    ) {
        List<BackofficeOrganizationPermissionResponse> permissionResponses = sortFeatures(permissions).stream()
                .map(this::toOrganizationPermissionResponse)
                .toList();

        return BackofficeOrganizationRoleDetailsResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .taskRole(entity.getTaskRole() != null ? entity.getTaskRole().name() : null)
                .isDefault(Boolean.TRUE.equals(entity.getIsDefault()))
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .permissions(permissionResponses)
                .build();
    }

    private BackofficeOrganizationPermissionResponse toOrganizationPermissionResponse(FeatureEntity feature) {
        return BackofficeOrganizationPermissionResponse.builder()
                .id(feature.getId())
                .code(feature.getCode())
                .name(feature.getName())
                .description(feature.getDescription())
                .category(feature.getCategory())
                .isTransaction(Boolean.TRUE.equals(feature.getIsTransaction()))
                .enabled(Boolean.TRUE.equals(feature.getEnabled()))
                .build();
    }

    private List<OrgRoleEntity> sortOrganizationRoles(List<OrgRoleEntity> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .sorted((first, second) -> {
                    boolean firstDefault = Boolean.TRUE.equals(first.getIsDefault());
                    boolean secondDefault = Boolean.TRUE.equals(second.getIsDefault());
                    if (firstDefault != secondDefault) {
                        return firstDefault ? -1 : 1;
                    }

                    boolean firstMaker = first.getTaskRole() == TaskRole.MAKER;
                    boolean secondMaker = second.getTaskRole() == TaskRole.MAKER;
                    if (firstMaker != secondMaker) {
                        return firstMaker ? -1 : 1;
                    }

                    String firstName = String.valueOf(first.getName()).toUpperCase();
                    String secondName = String.valueOf(second.getName()).toUpperCase();
                    return firstName.compareTo(secondName);
                })
                .toList();
    }

    private List<FeatureEntity> sortFeatures(List<FeatureEntity> features) {
        if (features == null || features.isEmpty()) {
            return List.of();
        }
        return features.stream()
                .filter(Objects::nonNull)
                .sorted((first, second) -> {
                    String firstCategory = String.valueOf(first.getCategory()).toUpperCase();
                    String secondCategory = String.valueOf(second.getCategory()).toUpperCase();
                    int categoryCompare = firstCategory.compareTo(secondCategory);
                    if (categoryCompare != 0) {
                        return categoryCompare;
                    }

                    String firstName = String.valueOf(first.getName()).toUpperCase();
                    String secondName = String.valueOf(second.getName()).toUpperCase();
                    int nameCompare = firstName.compareTo(secondName);
                    if (nameCompare != 0) {
                        return nameCompare;
                    }

                    return String.valueOf(first.getCode()).toUpperCase()
                            .compareTo(String.valueOf(second.getCode()).toUpperCase());
                })
                .toList();
    }

    private OrganizationEntity getRequiredOrganization(String clientId) {
        String normalizedClientId = normalizeClientId(clientId);
        return organizationRepository.findByParty_CoreCustomerId(normalizedClientId)
                .orElseThrow(() -> BaseException.notFound("Organization " + normalizedClientId + " not found."));
    }

    private OrganizationUserEntity getRequiredOrganizationUser(String clientId, Long organizationUserId) {
        String normalizedClientId = normalizeClientId(clientId);
        if (organizationUserId == null) {
            throw BaseException.badRequest("organizationUserId is required.");
        }

        OrganizationUserEntity organizationUser = organizationUserRepository.findById(organizationUserId)
                .orElseThrow(() -> BaseException.notFound("Organization user not found."));

        String userOrganizationClientId = organizationUser.getOrganizationParty() != null
                ? organizationUser.getOrganizationParty().getCoreCustomerId()
                : null;
        if (!StringUtils.hasText(userOrganizationClientId)
                || !normalizedClientId.equalsIgnoreCase(userOrganizationClientId.trim())) {
            throw BaseException.notFound("Organization user not found for client ID " + normalizedClientId + ".");
        }

        return organizationUser;
    }

    private IamUserEntity requireOrganizationUserIamUser(OrganizationUserEntity organizationUser) {
        IamUserEntity iamUser = organizationUser.getIamUser();
        if (iamUser == null || iamUser.getId() == null) {
            throw BaseException.badRequest("Organization user is not linked to an IAM user.");
        }
        return iamUser;
    }

    private CustomerAuthEntity requireCustomerAuth(IamUserEntity iamUser) {
        return customerAuthRepository.findByIamUserId(iamUser.getId())
                .orElseThrow(() -> BaseException.notFound("Customer credentials not found for organization user."));
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

    private boolean isVerifiedIndividual(IamUserEntity iamUser) {
        return iamUser != null
                && iamUser.getCustomerProfile() != null
                && Boolean.TRUE.equals(iamUser.getCustomerProfile().getIsVerified());
    }

    private String resolvePrimaryContact(IamUserEntity iamUser, ContactType type) {
        if (iamUser == null || type == null) {
            return null;
        }
        return userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, type)
                .map(UserContact::getContactValue)
                .orElse(null);
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

    private void sendPasswordResetNotification(
            IamUserEntity iamUser,
            OrganizationUserEntity organizationUser,
            String temporaryPassword
    ) {
        String email = resolvePrimaryContact(iamUser, ContactType.EMAIL);
        if (!StringUtils.hasText(email)) {
            return;
        }

        String fullName = iamUser.getParty() != null && iamUser.getParty().getPerson() != null
                ? iamUser.getParty().getPerson().getFullName()
                : "Customer";
        String reference = organizationUser.getOrganizationParty() != null
                ? organizationUser.getOrganizationParty().getCoreCustomerId()
                : null;

        try {
            notificationService.sendAdminPasswordResetNotice(email, fullName, reference, temporaryPassword);
        } catch (Exception exception) {
            log.warn("Password reset notification failed for org user {}: {}",
                    organizationUser.getId(), exception.getMessage());
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

        return resolvePersonNames(response.getFullName());
    }

    private String[] resolvePersonNames(String fullNameValue) {
        String fullName = trimToNull(fullNameValue);
        if (fullName == null) {
            throw BaseException.badRequest("Full name is required.");
        }

        String[] parts = fullName.split("\\s+");
        if (parts.length == 0) {
            throw BaseException.badRequest("Full name is required.");
        }

        String first = parts[0];
        String middle;
        String last;
        if (parts.length == 1) {
            last = parts[0];
            middle = null;
        } else {
            last = parts[parts.length - 1];
            middle = parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1)) : null;
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

    private void validateFilters(HttpServletRequest request) {
        String smeMode = firstNonBlank(request.getParameter("smeMode"));
        if (StringUtils.hasText(smeMode)
                && !"true".equalsIgnoreCase(smeMode.trim())
                && !"false".equalsIgnoreCase(smeMode.trim())) {
            throw BaseException.badRequest("smeMode must be either true or false.");
        }
    }

    private String normalizeClientId(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw BaseException.badRequest("Client ID is required.");
        }
        return clientId.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private record SearchCriteria(
            String customerId,
            String phone,
            String idNumber,
            String email,
            String passport
    ) {
        private boolean hasAny() {
            return StringUtils.hasText(customerId)
                    || StringUtils.hasText(phone)
                    || StringUtils.hasText(idNumber)
                    || StringUtils.hasText(email)
                    || StringUtils.hasText(passport);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
