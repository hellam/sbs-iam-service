package ke.shiva.sbs_iam.config.seeder;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.ProfileContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginProfiles;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Database seeder to create a customer profile for internet banking access
 * Uses the same IAM user created by EmployeeSeeder
 * Creates: LoginIdentifier -> CustomerProfile -> CustomerAuth
 *
 * Default credentials: username: user, password: user
 *
 * To enable this seeder, add the following to your application.yaml:
 * seeder:
 *   customer:
 *     enabled: true
 */
@Slf4j
@Component
@Order(2) // Run after EmployeeSeeder
@RequiredArgsConstructor
public class CustomerSeeder implements CommandLineRunner {

    @Value("${seeder.customer.enabled:false}")
    private boolean seederEnabled;

    private final LoginIdentifierRepository loginIdentifierRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final ProfileContactRepository profileContactRepository;
    private final UserContactRepository userContactRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seederEnabled) {
            log.debug("Customer seeder is disabled. Set 'seeder.customer.enabled=true' to enable it.");
            return;
        }

        log.info("Starting customer seeder...");

        // Check if customer user already exists
        if (loginIdentifierRepository.findByIdentifierAndIdentifierType("user", "username").isPresent()) {
            log.info("Customer user already exists. Skipping seeder.");
            return;
        }

        try {
            // Find the existing IAM user (created by EmployeeSeeder)
            LoginIdentifierEntity adminIdentifier = loginIdentifierRepository
                    .findByIdentifierAndIdentifierType("admin", "username")
                    .orElseThrow(() -> new RuntimeException("Admin user not found. Please run EmployeeSeeder first."));

            IamUserEntity iamUser = adminIdentifier.getIamUser();
            log.info("Found existing IAM user with ID: {}", iamUser.getId());

            // Create Login Identifier for Internet Banking (username)
            log.info("Creating login identifier: user");
            LoginIdentifierEntity loginIdentifier = new LoginIdentifierEntity();
            loginIdentifier.setIamUser(iamUser);
            loginIdentifier.setChannel(Channel.INTERNET_BANKING);
            loginIdentifier.setIdentifierType("username");
            loginIdentifier.setIdentifier("user");
            loginIdentifier.setStatus(IamStatus.ACTIVE);
            loginIdentifierRepository.save(loginIdentifier);

            // Link contacts to customer profile
            log.info("Linking contacts to customer profile");

            // Find existing user contacts
            var phoneContact = userContactRepository
                    .findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, ContactType.PHONE)
                    .orElseThrow(() -> new RuntimeException("Primary phone contact not found"));

            var emailContact = userContactRepository
                    .findByIamUserAndContactTypeAndPrimaryIsTrue(iamUser, ContactType.EMAIL)
                    .orElseThrow(() -> new RuntimeException("Primary email contact not found"));

            // Phone Profile Contact
            ProfileContact phoneProfileContact = new ProfileContact();
            phoneProfileContact.setIamUser(iamUser);
            phoneProfileContact.setUserContact(phoneContact);
            phoneProfileContact.setProfileType(LoginProfiles.CUSTOMER);
            phoneProfileContact.setContactType(ContactType.PHONE);
            profileContactRepository.save(phoneProfileContact);

            // Email Profile Contact
            ProfileContact emailProfileContact = new ProfileContact();
            emailProfileContact.setIamUser(iamUser);
            emailProfileContact.setUserContact(emailContact);
            emailProfileContact.setProfileType(LoginProfiles.CUSTOMER);
            emailProfileContact.setContactType(ContactType.EMAIL);
            profileContactRepository.save(emailProfileContact);

            // Create Customer Profile
            log.info("Creating customer profile");
            CustomerProfileEntity customerProfile = new CustomerProfileEntity();
            customerProfile.setIamUser(iamUser);
            customerProfile.setCoreCustomerId("CUST001");
            customerProfile.setSegment("RETAIL");
            customerProfile.setLanguage("en");
            customerProfile.setTimezone("Africa/Nairobi");
            customerProfile.setTheme("light");
            customerProfile.setAllowEmail(true);
            customerProfile.setAllowSms(true);
            customerProfile.setAllowPush(false);
            customerProfile.setCreatedAt(LocalDateTime.now());
            customerProfile.setUpdatedAt(LocalDateTime.now());
            customerProfileRepository.save(customerProfile);

            // Create Customer Auth (password: user)
            log.info("Creating customer auth with password: user");
            CustomerAuthEntity customerAuth = new CustomerAuthEntity();
            customerAuth.setIamUser(iamUser);

            // Internet banking credentials
            String hashedPassword = HashUtil.bcrypt("user");
            customerAuth.setInternetPasswordHash(hashedPassword);
            customerAuth.setInternetPasswordAlgo("bcrypt");
            customerAuth.setInternetPasswordChangedAt(OffsetDateTime.now());
            customerAuth.setInternetFirstTimeLogin(false);
            customerAuth.setInternetFailedAttempts((short) 0);
            customerAuth.setInternetLocked(false);

            // Mobile banking credentials (not set initially)
            customerAuth.setMobileFirstTimeLogin(true);
            customerAuth.setMobileFailedAttempts((short) 0);
            customerAuth.setMobileLocked(false);

            // MFA settings
            customerAuth.setMfaEnabled(false);

            customerAuthRepository.save(customerAuth);

            log.info("Customer seeder completed successfully!");
            log.info("Created customer with username: user, password: user");
            log.info("Core Customer ID: CUST001");
            log.info("Customer can now access via Internet Banking channel");

        } catch (Exception e) {
            log.error("Error running customer seeder", e);
            throw new RuntimeException("Failed to seed customer data", e);
        }
    }
}
