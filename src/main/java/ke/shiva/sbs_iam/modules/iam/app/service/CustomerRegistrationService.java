package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.IamRegistrationDetailsRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final LoginIdentifierRepository loginIdentifierRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final ProfileContactRepository profileContactRepository;
    private final UserContactRepository userContactRepository;
    private final IamUserRepository iamUserRepository;

    @Transactional
    public Void registerCustomer(IamRegistrationDetailsRequest request) {
        log.info("Starting IAM customer registration for client ID: {}", request.getClientId());

        // 1. Check for existing user (e.g., by username or client ID)
        //TODO generate username, check for channel if exists, if not generate and check for uniqueness, if exists throw exception, else proceed with registration
//        if (loginIdentifierRepository.findByIdentifierAndChannel(request.getChannel(), "username").isPresent()) {
//            throw new RegistrationValidationException("Username '" + request.getUsername() + "' is already taken.");
//        }
//        if (customerProfileRepository.findByCoreCustomerId(request.getClientDetails().getClientId()).isPresent()) {
//            throw new RegistrationValidationException("Client ID '" + request.getClientDetails().getClientId() + "' is already registered.");
//        }

        // 2. Create IamUserEntity
        IamUserEntity iamUser = new IamUserEntity();
//        iamUser.setFirstName(request.getFirstName());
//        iamUser.setLastName(request.getLastName());
//        iamUser.setMiddleName(request.getMiddleName());
//        iamUser.setNationalId(request.getNationalId());
        iamUser.setStatus(IamStatus.ACTIVE);
        iamUser.setCreatedAt(OffsetDateTime.now());
        iamUser.setUpdatedAt(OffsetDateTime.now());
        iamUserRepository.save(iamUser);

        // 3. Create LoginIdentifier for username
        LoginIdentifierEntity loginIdentifier = new LoginIdentifierEntity();
        loginIdentifier.setIamUser(iamUser);
        loginIdentifier.setChannel(Channel.INTERNET_BANKING);
        loginIdentifier.setIdentifierType("username");
//        loginIdentifier.setIdentifier(request.getUsername());
        loginIdentifier.setStatus(IamStatus.ACTIVE);
        loginIdentifierRepository.save(loginIdentifier);

        // 4. Create UserContact for phone number
//        UserContactEntity phoneContact = new UserContactEntity();
//        phoneContact.setIamUser(iamUser);
//        phoneContact.setContactType(ContactType.PHONE);
//        phoneContact.setContactValue(request.getPhoneNumber());
//        phoneContact.setPrimary(true);
//        phoneContact.setVerified(true); // Assuming verified after core banking validation
//        phoneContact.setCreatedAt(LocalDateTime.now());
//        phoneContact.setUpdatedAt(LocalDateTime.now());
//        userContactRepository.save(phoneContact);

        // 5. Create UserContact for email
//        UserContactEntity emailContact = new UserContactEntity();
//        emailContact.setIamUser(iamUser);
//        emailContact.setContactType(ContactType.EMAIL);
//        emailContact.setContactValue(request.getEmail());
//        emailContact.setPrimary(true);
//        emailContact.setVerified(true); // Assuming verified after core banking validation
//        emailContact.setCreatedAt(LocalDateTime.now());
//        emailContact.setUpdatedAt(LocalDateTime.now());
//        userContactRepository.save(emailContact);

        // 6. Create ProfileContact for phone
        ProfileContact phoneProfileContact = new ProfileContact();
        phoneProfileContact.setIamUser(iamUser);
//        phoneProfileContact.setUserContact(phoneContact);
        phoneProfileContact.setProfileType(LoginProfiles.CUSTOMER);
        phoneProfileContact.setContactType(ContactType.PHONE);
        profileContactRepository.save(phoneProfileContact);

        // 7. Create ProfileContact for email
        ProfileContact emailProfileContact = new ProfileContact();
        emailProfileContact.setIamUser(iamUser);
//        emailProfileContact.setUserContact(emailContact);
        emailProfileContact.setProfileType(LoginProfiles.CUSTOMER);
        emailProfileContact.setContactType(ContactType.EMAIL);
        profileContactRepository.save(emailProfileContact);

        // 8. Create CustomerProfileEntity
//        ClientDetailsResponse clientDetails = request.getClientDetails();
        CustomerProfileEntity customerProfile = new CustomerProfileEntity();
        customerProfile.setIamUser(iamUser);
        customerProfile.setCoreCustomerId(request.getClientId());
        customerProfile.setSegment("RETAIL"); // Default segment, can be derived from clientDetails if available
        customerProfile.setLanguage("en");
        customerProfile.setTimezone("Africa/Nairobi"); // Default timezone
        customerProfile.setTheme("light"); // Default theme
        customerProfile.setAllowEmail(true);
        customerProfile.setAllowSms(true);
        customerProfile.setAllowPush(false);
        customerProfile.setCreatedAt(LocalDateTime.now());
        customerProfile.setUpdatedAt(LocalDateTime.now());
        customerProfileRepository.save(customerProfile);

        // 9. Create CustomerAuthEntity
        CustomerAuthEntity customerAuth = new CustomerAuthEntity();
        customerAuth.setIamUser(iamUser);
//        customerAuth.setInternetPasswordHash(HashUtil.bcrypt(request.getPassword()));
        customerAuth.setInternetPasswordAlgo("bcrypt");
        customerAuth.setInternetPasswordChangedAt(OffsetDateTime.now());
        customerAuth.setInternetFirstTimeLogin(true); // First time login for new user
        customerAuth.setInternetFailedAttempts((short) 0);
        customerAuth.setInternetLocked(false);
        customerAuth.setMobileFirstTimeLogin(true);
        customerAuth.setMobileFailedAttempts((short) 0);
        customerAuth.setMobileLocked(false);
        customerAuth.setMfaEnabled(false);
        customerAuthRepository.save(customerAuth);

        log.info("IAM customer registration successful for client ID: {}", request.getClientId());

        return null; // Void return type
    }
}
