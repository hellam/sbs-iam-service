package ke.shiva.sbs_iam.config.seeder;

import ke.shiva.client.iam.enums.TaskRole;
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
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.FeatureRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrganizationUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrgRolePermissionRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.OrgRoleRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PartyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PersonRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class CorporateSeeder implements CommandLineRunner {

    @Value("${seeder.corporate.enabled:false}")
    private boolean seederEnabled;

    @Value("${seeder.corporate.default-password:ChangeMe123!}")
    private String defaultPassword;

    @Value("${seeder.corporate.reset-password-on-run:true}")
    private boolean resetPasswordOnRun;

    @Value("${seeder.corporate.organization.core-customer-id:101058}")
    private String organizationCoreCustomerId;

    @Value("${seeder.corporate.organization.legal-name:SHIVA SOFTWARES AFRICA LTD}")
    private String organizationLegalName;

    @Value("${seeder.corporate.organization.display-name:SHIVA SOFTWARES AFRICA LTD}")
    private String organizationDisplayName;

    @Value("${seeder.corporate.organization.customer-segment:CORPORATE}")
    private String organizationSegment;

    @Value("${seeder.corporate.organization.registration-no:SHIVA-101058}")
    private String organizationRegistrationNo;

    @Value("${seeder.corporate.organization.country-code:KE}")
    private String organizationCountryCode;

    @Value("${seeder.corporate.organization.city:Nairobi}")
    private String organizationCity;

    @Value("${seeder.corporate.organization.address:Nairobi, Kenya}")
    private String organizationAddress;

    @Value("${seeder.corporate.organization.company-email:ops@shiva.test}")
    private String organizationEmail;

    @Value("${seeder.corporate.organization.company-phone:+254700101058}")
    private String organizationPhone;

    @Value("${seeder.corporate.roles.maker.name:Maker}")
    private String makerRoleName;

    @Value("${seeder.corporate.roles.maker.description:Initiates and submits corporate transactions.}")
    private String makerRoleDescription;

    @Value("${seeder.corporate.roles.checker.name:Checker}")
    private String checkerRoleName;

    @Value("${seeder.corporate.roles.checker.description:Reviews and verifies submitted corporate transactions.}")
    private String checkerRoleDescription;

    @Value("${seeder.corporate.roles.approver.name:Approver}")
    private String approverRoleName;

    @Value("${seeder.corporate.roles.approver.description:Performs final transaction approval.}")
    private String approverRoleDescription;

    private final PartyRepository partyRepository;
    private final PersonRepository personRepository;
    private final IamUserRepository iamUserRepository;
    private final LoginIdentifierRepository loginIdentifierRepository;
    private final UserContactRepository userContactRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final OrgRoleRepository orgRoleRepository;
    private final OrgRolePermissionRepository orgRolePermissionRepository;
    private final FeatureRepository featureRepository;
    private final CountryRepository countryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seederEnabled) {
            log.debug("Corporate seeder is disabled. Set 'seeder.corporate.enabled=true' to enable it.");
            return;
        }

        log.info("Starting corporate seeder for customerId={} legalName={}",
                organizationCoreCustomerId, organizationLegalName);

        PartyEntity organizationParty = upsertOrganization();
        Map<TaskRole, OrgRoleEntity> orgRoles = upsertRoles(organizationParty);
        Map<String, FeatureEntity> features = upsertFeatures();
        seedRolePermissions(orgRoles, features);
        seedCorporateUsers(organizationParty, orgRoles);

        log.info("Corporate seeder completed. customerId={}, orgPartyId={}",
                organizationCoreCustomerId, organizationParty.getId());
    }

    private PartyEntity upsertOrganization() {
        CountryEntity country = countryRepository.findByCountryCode(organizationCountryCode)
                .orElse(null);
        if (country == null) {
            log.warn("Country code {} not found. Organization will be seeded without country reference.",
                    organizationCountryCode);
        }

        Optional<OrganizationEntity> existing = organizationRepository.findByLegalNameIgnoreCase(organizationLegalName);
        if (existing.isPresent()) {
            OrganizationEntity organization = existing.get();
            PartyEntity party = organization.getParty();

            party.setPartyType(PartyType.ORGANIZATION);
            party.setStatus("ACTIVE");
            party.setCoreCustomerId(organizationCoreCustomerId);
            partyRepository.save(party);

            organization.setDisplayName(organizationDisplayName);
            organization.setRegistrationNo(organizationRegistrationNo);
            organization.setCustomerSegment(organizationSegment);
            organization.setSmeMode(false);
            organization.setCountryCode(country);
            organization.setAddress(organizationAddress);
            organization.setCity(organizationCity);
            organization.setCompanyEmail(organizationEmail);
            organization.setCompanyPhone(organizationPhone);
            organization.setUpdatedAt(OffsetDateTime.now());
            if (organization.getCreatedAt() == null) {
                organization.setCreatedAt(OffsetDateTime.now());
            }
            organizationRepository.save(organization);

            return party;
        }

        PartyEntity party = new PartyEntity();
        party.setPublicId(UUID.randomUUID());
        party.setPartyType(PartyType.ORGANIZATION);
        party.setCoreCustomerId(organizationCoreCustomerId);
        party.setStatus("ACTIVE");
        party = partyRepository.save(party);

        OrganizationEntity organization = new OrganizationEntity();
        organization.setParty(party);
        organization.setLegalName(organizationLegalName);
        organization.setDisplayName(organizationDisplayName);
        organization.setRegistrationNo(organizationRegistrationNo);
        organization.setCustomerSegment(organizationSegment);
        organization.setSmeMode(false);
        organization.setCountryCode(country);
        organization.setAddress(organizationAddress);
        organization.setCity(organizationCity);
        organization.setCompanyEmail(organizationEmail);
        organization.setCompanyPhone(organizationPhone);
        organization.setCreatedAt(OffsetDateTime.now());
        organization.setUpdatedAt(OffsetDateTime.now());
        organizationRepository.save(organization);

        return party;
    }

    private Map<TaskRole, OrgRoleEntity> upsertRoles(PartyEntity organizationParty) {
        Map<TaskRole, OrgRoleEntity> roles = new LinkedHashMap<>();
        roles.put(TaskRole.MAKER, upsertRole(
                organizationParty,
                TaskRole.MAKER,
                makerRoleName,
                makerRoleDescription,
                true
        ));
        roles.put(TaskRole.CHECKER, upsertRole(
                organizationParty,
                TaskRole.CHECKER,
                checkerRoleName,
                checkerRoleDescription,
                false
        ));
        roles.put(TaskRole.APPROVER, upsertRole(
                organizationParty,
                TaskRole.APPROVER,
                approverRoleName,
                approverRoleDescription,
                false
        ));
        return roles;
    }

    private OrgRoleEntity upsertRole(
            PartyEntity organizationParty,
            TaskRole taskRole,
            String name,
            String description,
            boolean isDefault
    ) {
        OrgRoleEntity role = orgRoleRepository
                .findByOrganizationPartyAndTaskRole(organizationParty, taskRole)
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
        role.setIsDefault(isDefault);
        role.setIsActive(true);
        role.setUpdatedAt(OffsetDateTime.now());

        return orgRoleRepository.save(role);
    }

    private Map<String, FeatureEntity> upsertFeatures() {
        List<FeatureSeed> featureSeeds = List.of(
                new FeatureSeed("INTERNAL_TRANSFER", "Internal Transfer", "Initiate internal account transfers.", "Transfers", true),
                new FeatureSeed("RTGS_TRANSFER", "RTGS Transfer", "Initiate RTGS transfers.", "Transfers", true),
                new FeatureSeed("SWIFT_TRANSFER", "SWIFT Transfer", "Initiate SWIFT transfers.", "Transfers", true),
                new FeatureSeed("APPROVAL_QUEUE", "Approval Queue", "View and action pending approvals.", "Approvals", false)
        );

        Map<String, FeatureEntity> featuresByCode = new LinkedHashMap<>();
        for (FeatureSeed seed : featureSeeds) {
            FeatureEntity feature = featureRepository.findByCode(seed.code())
                    .orElseGet(FeatureEntity::new);

            if (feature.getId() == null) {
                feature.setCode(seed.code());
                feature.setCreatedAt(OffsetDateTime.now());
            }

            feature.setChannel(Channel.INTERNET_BANKING);
            feature.setName(seed.name());
            feature.setDescription(seed.description());
            feature.setCategory(seed.category());
            feature.setEnabled(true);
            feature.setIsTransaction(seed.isTransaction());

            featuresByCode.put(seed.code(), featureRepository.save(feature));
        }
        return featuresByCode;
    }

    private void seedRolePermissions(Map<TaskRole, OrgRoleEntity> roles, Map<String, FeatureEntity> features) {
        Map<TaskRole, List<String>> grants = Map.of(
                TaskRole.MAKER, List.of("INTERNAL_TRANSFER", "RTGS_TRANSFER", "SWIFT_TRANSFER"),
                TaskRole.CHECKER, List.of("INTERNAL_TRANSFER", "RTGS_TRANSFER", "SWIFT_TRANSFER", "APPROVAL_QUEUE"),
                TaskRole.APPROVER, List.of("INTERNAL_TRANSFER", "RTGS_TRANSFER", "SWIFT_TRANSFER", "APPROVAL_QUEUE")
        );

        for (Map.Entry<TaskRole, List<String>> entry : grants.entrySet()) {
            OrgRoleEntity role = roles.get(entry.getKey());
            if (role == null) {
                continue;
            }

            for (String featureCode : entry.getValue()) {
                FeatureEntity feature = features.get(featureCode);
                if (feature == null) {
                    continue;
                }

                OrgRolePermissionIdEntity id = new OrgRolePermissionIdEntity();
                id.setOrgRoleId(role.getId());
                id.setFeatureId(feature.getId());

                if (orgRolePermissionRepository.existsById(id)) {
                    continue;
                }

                OrgRolePermissionEntity permission = new OrgRolePermissionEntity();
                permission.setId(id);
                permission.setOrgRole(role);
                permission.setFeature(feature);
                orgRolePermissionRepository.save(permission);
            }
        }
    }

    private void seedCorporateUsers(PartyEntity organizationParty, Map<TaskRole, OrgRoleEntity> orgRoles) {
        String suffix = organizationCoreCustomerId;

        List<CorporateUserSeed> users = List.of(
                new CorporateUserSeed(
                        TaskRole.MAKER,
                        "Mary",
                        "Inputter",
                        "CID-" + suffix + "-MKR",
                        "corp.maker." + suffix,
                        "+254700101061",
                        "corp.maker." + suffix + "@shiva.test",
                        true
                ),
                new CorporateUserSeed(
                        TaskRole.CHECKER,
                        "Kevin",
                        "Verifier",
                        "CID-" + suffix + "-CHK",
                        "corp.checker." + suffix,
                        "+254700101062",
                        "corp.checker." + suffix + "@shiva.test",
                        false
                ),
                new CorporateUserSeed(
                        TaskRole.APPROVER,
                        "Alice",
                        "Authorizer",
                        "CID-" + suffix + "-APR",
                        "corp.approver." + suffix,
                        "+254700101063",
                        "corp.approver." + suffix + "@shiva.test",
                        false
                )
        );

        for (CorporateUserSeed seed : users) {
            IamUserEntity user = upsertCorporateIamUser(seed);
            upsertOrganizationUser(user, organizationParty, orgRoles.get(seed.taskRole()), seed.isPrimary());
        }
    }

    private IamUserEntity upsertCorporateIamUser(CorporateUserSeed seed) {
        LoginIdentifierEntity loginIdentifier = loginIdentifierRepository
                .findByIdentifierAndChannel(seed.username(), Channel.INTERNET_BANKING)
                .orElse(null);

        IamUserEntity iamUser;
        if (loginIdentifier != null) {
            iamUser = loginIdentifier.getIamUser();
            loginIdentifier.setStatus(IamStatus.ACTIVE);
            loginIdentifierRepository.save(loginIdentifier);
        } else {
            PartyEntity party = new PartyEntity();
            party.setPublicId(UUID.randomUUID());
            party.setPartyType(PartyType.PERSON);
            party.setStatus("ACTIVE");
            party = partyRepository.save(party);

            PersonEntity person = new PersonEntity();
            person.setParty(party);
            person.setFirstName(seed.firstName());
            person.setLastName(seed.lastName());
            person.setFullName(seed.firstName() + " " + seed.lastName());
            person.setNationalId(seed.nationalId());
            person.setCity(organizationCity);
            person.setAddress(organizationAddress);
            person.setCreatedAt(OffsetDateTime.now());
            person.setUpdatedAt(OffsetDateTime.now());
            personRepository.save(person);

            iamUser = new IamUserEntity();
            iamUser.setPublicId(UUID.randomUUID());
            iamUser.setParty(party);
            iamUser.setAuthProvider("LOCAL");
            iamUser.setStatus(IamStatus.ACTIVE);
            iamUser.setCreatedAt(OffsetDateTime.now());
            iamUser.setUpdatedAt(OffsetDateTime.now());
            iamUser = iamUserRepository.save(iamUser);

            LoginIdentifierEntity newLoginIdentifier = new LoginIdentifierEntity();
            newLoginIdentifier.setIamUser(iamUser);
            newLoginIdentifier.setChannel(Channel.INTERNET_BANKING);
            newLoginIdentifier.setIdentifierType("username");
            newLoginIdentifier.setIdentifier(seed.username());
            newLoginIdentifier.setStatus(IamStatus.ACTIVE);
            loginIdentifierRepository.save(newLoginIdentifier);
        }

        upsertContact(iamUser, ContactType.PHONE, seed.phone());
        upsertContact(iamUser, ContactType.EMAIL, seed.email());
        upsertCustomerAuth(iamUser);

        return iamUser;
    }

    private void upsertContact(IamUserEntity iamUser, ContactType contactType, String value) {
        UserContact contact = userContactRepository.findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, contactType)
                .orElseGet(UserContact::new);

        contact.setIamUser(iamUser);
        contact.setContactType(contactType);
        contact.setContactValue(value);
        contact.setPrimary(true);
        contact.setUpdatedAt(OffsetDateTime.now());
        if (contact.getCreatedAt() == null) {
            contact.setCreatedAt(OffsetDateTime.now());
        }
        userContactRepository.save(contact);
    }

    private void upsertCustomerAuth(IamUserEntity iamUser) {
        CustomerAuthEntity auth = customerAuthRepository.findByIamUserId(iamUser.getId())
                .orElseGet(CustomerAuthEntity::new);

        auth.setIamUser(iamUser);
        if (auth.getInternetPasswordHash() == null || resetPasswordOnRun) {
            auth.setInternetPasswordHash(HashUtil.bcrypt(defaultPassword));
        }
        auth.setInternetPasswordAlgo("bcrypt");
        auth.setInternetPasswordChangedAt(OffsetDateTime.now());
        auth.setInternetFirstTimeLogin(false);
        auth.setInternetFailedAttempts((short) 0);
        auth.setInternetLocked(false);

        auth.setMobileFirstTimeLogin(true);
        auth.setMobileFailedAttempts((short) 0);
        auth.setMobileLocked(false);

        auth.setMfaEnabled(false);
        if (auth.getCreatedAt() == null) {
            auth.setCreatedAt(OffsetDateTime.now());
        }
        auth.setUpdatedAt(OffsetDateTime.now());

        customerAuthRepository.save(auth);
    }

    private void upsertOrganizationUser(
            IamUserEntity iamUser,
            PartyEntity organizationParty,
            OrgRoleEntity orgRole,
            boolean isPrimary
    ) {
        if (orgRole == null) {
            throw new IllegalStateException("Org role is required when seeding organization_user");
        }

        OrganizationUserEntity organizationUser = organizationUserRepository
                .findByIamUserAndOrganizationParty(iamUser, organizationParty)
                .orElseGet(OrganizationUserEntity::new);

        organizationUser.setIamUser(iamUser);
        organizationUser.setOrganizationParty(organizationParty);
        organizationUser.setOrgRole(orgRole);
        organizationUser.setIsPrimary(isPrimary);
        organizationUser.setStatus("ACTIVE");
        if (organizationUser.getCreatedAt() == null) {
            organizationUser.setCreatedAt(OffsetDateTime.now());
        }
        organizationUser.setUpdatedAt(OffsetDateTime.now());
        organizationUserRepository.save(organizationUser);
    }

    private record FeatureSeed(
            String code,
            String name,
            String description,
            String category,
            boolean isTransaction
    ) {
    }

    private record CorporateUserSeed(
            TaskRole taskRole,
            String firstName,
            String lastName,
            String nationalId,
            String username,
            String phone,
            String email,
            boolean isPrimary
    ) {
    }
}
