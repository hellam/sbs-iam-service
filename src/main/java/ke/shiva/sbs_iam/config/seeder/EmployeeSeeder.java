package ke.shiva.sbs_iam.config.seeder;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.employee.EmploymentStatus;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.domain.enums.BranchTypeEnum;
import ke.shiva.sbs_iam.modules.reference.infra.repository.BranchRepository;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Database seeder to create an initial employee in the system
 * Creates: Country (Kenya) -> Branch -> Party -> Person -> IamUser -> LoginIdentifier -> EmployeeProfile -> EmployeeAuth
 *
 * To enable this seeder, add the following to your application.yaml:
 * seeder:
 *   employee:
 *     enabled: true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeSeeder implements CommandLineRunner {

    @Value("${seeder.employee.enabled:false}")
    private boolean seederEnabled;

    private final CountryRepository countryRepository;
    private final BranchRepository branchRepository;
    private final PartyRepository partyRepository;
    private final PersonRepository personRepository;
    private final IamUserRepository iamUserRepository;
    private final LoginIdentifierRepository loginIdentifierRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeAuthRepository employeeAuthRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seederEnabled) {
            log.debug("Employee seeder is disabled. Set 'seeder.employee.enabled=true' to enable it.");
            return;
        }

        log.info("Starting employee seeder...");

        // Check if admin user already exists
        if (loginIdentifierRepository.findByIdentifierAndIdentifierType("admin", "username").isPresent()) {
            log.info("Admin user already exists. Skipping seeder.");
            return;
        }

        try {
            // Check if country already exists
            CountryEntity country = countryRepository.findByCountryCode("KE")
                    .orElseGet(() -> {
                        log.info("Creating country: Kenya");
                        CountryEntity newCountry = new CountryEntity();
                        newCountry.setCountryCode("KE");
                        newCountry.setPhoneCode("+254");
                        newCountry.setCountryName("Kenya");
                        newCountry.setCurrencyCode("KES");
                        newCountry.setCurrencyName("Kenyan Shilling");
                        return countryRepository.save(newCountry);
                    });

            // Check if branch already exists
            BranchEntity branch = branchRepository.findByBranchCode("HQ001")
                    .orElseGet(() -> {
                        log.info("Creating branch: Headquarters");
                        BranchEntity newBranch = new BranchEntity();
                        newBranch.setBranchCode("HQ001");
                        newBranch.setBranchName("Headquarters");
                        newBranch.setBranchTypeEnum(BranchTypeEnum.BRANCH);
                        newBranch.setCountryCode(country);
                        newBranch.setAddress("Nairobi CBD");
                        newBranch.setCity("Nairobi");
                        newBranch.setCreatedBy("SYSTEM");
                        return branchRepository.save(newBranch);
                    });

            // Create Party
            log.info("Creating party for employee");
            PartyEntity party = new PartyEntity();
            party.setPublicId(UUID.randomUUID());
            party.setPartyType("PERSON");
            party.setStatus("ACTIVE");
            party = partyRepository.save(party);

            // Create Person
            log.info("Creating person: Admin User");
            PersonEntity person = new PersonEntity();
            person.setParty(party);
            person.setFirstName("Admin");
            person.setLastName("User");
            person.setFullName("Admin User");
            person.setNationalId("12345678");
            person.setGender("MALE");
            person.setCountryCode(country);
            person.setPhone("+254712345678");
            person.setEmail("admin@shiva-banking.ke");
            person.setAddress("Nairobi");
            person.setCity("Nairobi");
            person.setCreatedAt(OffsetDateTime.now());
            person.setUpdatedAt(OffsetDateTime.now());
            person = personRepository.save(person);

            // Create IAM User
            log.info("Creating IAM user");
            IamUserEntity iamUser = new IamUserEntity();
            iamUser.setPublicId(UUID.randomUUID());
            iamUser.setParty(party);
            iamUser.setAuthProvider("LOCAL");
            iamUser.setStatus(IamStatus.ACTIVE);
            iamUser.setCreatedAt(OffsetDateTime.now());
            iamUser.setUpdatedAt(OffsetDateTime.now());
            iamUser = iamUserRepository.save(iamUser);

            // Create Login Identifier (username)
            log.info("Creating login identifier: admin");
            LoginIdentifierEntity loginIdentifier = new LoginIdentifierEntity();
            loginIdentifier.setIamUser(iamUser);
            loginIdentifier.setChannel(Channel.BACKOFFICE);
            loginIdentifier.setIdentifierType("username");
            loginIdentifier.setIdentifier("admin");
            loginIdentifier.setStatus(IamStatus.ACTIVE);
            loginIdentifierRepository.save(loginIdentifier);

            // Create Employee Profile
            log.info("Creating employee profile");
            EmployeeProfileEntity employeeProfile = new EmployeeProfileEntity();
            employeeProfile.setIamUser(iamUser);
            employeeProfile.setStaffNo("EMP001");
            employeeProfile.setJobTitle("System Administrator");
            employeeProfile.setDepartment("IT");
            employeeProfile.setEmploymentStatus(EmploymentStatus.ACTIVE);
            employeeProfile.setBranch(branch.getId());
            employeeProfile.setCreatedAt(OffsetDateTime.now());
            employeeProfile.setUpdatedAt(OffsetDateTime.now());
            employeeProfileRepository.save(employeeProfile);

            // Create Employee Auth (password: admin)
            log.info("Creating employee auth with password: admin");
            EmployeeAuthEntity employeeAuth = new EmployeeAuthEntity();
            employeeAuth.setIamUser(iamUser);
            String hashedPassword = HashUtil.bcrypt("admin");
            employeeAuth.setStaffPasswordHash(hashedPassword);
            employeeAuth.setStaffPasswordAlgo("bcrypt");
            employeeAuth.setStaffFailedAttempts((short) 0);
            employeeAuth.setStaffLocked(false);
            employeeAuth.setMfaEnabled(false);
            employeeAuthRepository.save(employeeAuth);

            log.info("Employee seeder completed successfully!");
            log.info("Created employee with username: admin, password: admin");
            log.info("Staff No: EMP001");
            log.info("Branch: {} ({})", branch.getBranchName(), branch.getBranchCode());

        } catch (Exception e) {
            log.error("Error running employee seeder", e);
            throw new RuntimeException("Failed to seed employee data", e);
        }
    }
}

