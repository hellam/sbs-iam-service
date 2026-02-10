package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.corebanking.exception.RegistrationValidationException;
import ke.shiva.sbs_iam.modules.iam.api.request.IamRegistrationDetailsRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PersonEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.ProfileContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginProfiles;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final PartyRepository partyRepository;
    private final PersonRepository personRepository;
    private final IamUserRepository iamUserRepository;
    private final UserContactRepository userContactRepository;
    private final ProfileContactRepository profileContactRepository;
    private final LoginIdentifierRepository loginIdentifierRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final CountryRepository countryRepository;

    @Transactional
    public void registerCustomer(IamRegistrationDetailsRequest request) {

        log.info("Starting full IAM customer registration for clientId={}", request.getClientId());

        // 1. Check for existing user (e.g., by username or client ID)
        //TODO generate username, check for channel if exists, if not generate and check for uniqueness, if exists throw exception, else proceed with registration
//        if (loginIdentifierRepository.findByIdentifierAndChannel("username", request.getChannel()).isPresent()) {
//            throw new RegistrationValidationException("Username '" + request.getUsername() + "' is already taken.");
//        }

        /* -------------------------------------------------
         * 1. Create Party
         * ------------------------------------------------- */
        PartyEntity party = new PartyEntity();
        party.setPublicId(UUID.randomUUID());
        party.setPartyType(PartyType.PERSON);
        party.setStatus("ACTIVE");
        party = partyRepository.save(party);

        /* -------------------------------------------------
         * 2. Create Person
         * ------------------------------------------------- */
        PersonEntity person = new PersonEntity();
        person.setParty(party);
        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setFullName(
                request.getFirstName() + " " + request.getLastName()
        );
        person.setNationalId(request.getNationalId());
//        person.setCountryCode(getCountryCode(request.getCountry()));
        person.setCity(request.getCity());
        person.setAddress(request.getAddress());
        person.setCreatedAt(OffsetDateTime.now());
        person.setUpdatedAt(OffsetDateTime.now());
        personRepository.save(person);

        /* -------------------------------------------------
         * 3. Create IAM User
         * ------------------------------------------------- */
        IamUserEntity iamUser = new IamUserEntity();
        iamUser.setPublicId(UUID.randomUUID());
        iamUser.setParty(party);
        iamUser.setAuthProvider("LOCAL");
        iamUser.setStatus(IamStatus.ACTIVE);
        iamUser.setCreatedAt(OffsetDateTime.now());
        iamUser.setUpdatedAt(OffsetDateTime.now());
        iamUserRepository.save(iamUser);

        /* -------------------------------------------------
         * 4. Create User Contacts
         * ------------------------------------------------- */
        UserContact phone = new UserContact();
        phone.setIamUser(iamUser);
        phone.setContactType(ContactType.PHONE);
        phone.setContactValue(request.getMobile());
        phone.setPrimary(true);
        phone.setCreatedAt(OffsetDateTime.now());
        phone.setUpdatedAt(OffsetDateTime.now());
        userContactRepository.save(phone);

        UserContact email = new UserContact();
        email.setIamUser(iamUser);
        email.setContactType(ContactType.EMAIL);
        email.setContactValue(request.getEmail());
        email.setPrimary(true);
        email.setCreatedAt(OffsetDateTime.now());
        email.setUpdatedAt(OffsetDateTime.now());
        userContactRepository.save(email);

        /* -------------------------------------------------
         * 5. Link Contacts to CUSTOMER Profile
         * ------------------------------------------------- */
        ProfileContact phoneProfile = new ProfileContact();
        phoneProfile.setIamUser(iamUser);
        phoneProfile.setUserContact(phone);
        phoneProfile.setProfileType(LoginProfiles.CUSTOMER);
        phoneProfile.setContactType(ContactType.PHONE);
        profileContactRepository.save(phoneProfile);

        ProfileContact emailProfile = new ProfileContact();
        emailProfile.setIamUser(iamUser);
        emailProfile.setUserContact(email);
        emailProfile.setProfileType(LoginProfiles.CUSTOMER);
        emailProfile.setContactType(ContactType.EMAIL);
        profileContactRepository.save(emailProfile);

        /* -------------------------------------------------
         * 6. Create Login Identifier
         * ------------------------------------------------- */
        LoginIdentifierEntity loginIdentifier = new LoginIdentifierEntity();
        loginIdentifier.setIamUser(iamUser);
        loginIdentifier.setChannel(Channel.INTERNET_BANKING);
        loginIdentifier.setIdentifierType("username");
        loginIdentifier.setIdentifier(generateUniqueUsername(request.getFirstName(), request.getLastName()));
        loginIdentifier.setStatus(IamStatus.ACTIVE);
        loginIdentifierRepository.save(loginIdentifier);

        /* -------------------------------------------------
         * 7. Create Customer Profile
         * ------------------------------------------------- */
        CustomerProfileEntity customerProfile = new CustomerProfileEntity();
        customerProfile.setIamUser(iamUser);
        customerProfile.setCoreCustomerId(request.getClientId());
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

        /* -------------------------------------------------
         * 8. Create Customer Auth
         * ------------------------------------------------- */
        CustomerAuthEntity auth = new CustomerAuthEntity();
        auth.setIamUser(iamUser);
        auth.setInternetPasswordHash(HashUtil.bcrypt("123456"));
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

        log.info("Full IAM customer registration completed for clientId={}", request.getClientId());
    }

    private String generateUniqueUsername(
            String firstName,
            String lastName
    ) {
        String base =
                (firstName + "." + lastName)
                        .toLowerCase()
                        .replaceAll("[^a-z0-9.]", "");

        String candidate = base;
        int suffix = 1;

        while (loginIdentifierRepository
                .findByChannelAndIdentifierTypeAndIdentifier(
                        Channel.INTERNET_BANKING,
                        "username",
                        candidate
                ).isPresent()) {

            candidate = base + suffix;
            suffix++;
        }

        return candidate;
    }

    //generate country code from country name
    private CountryEntity getCountryCode(String countryName) {
        return countryRepository.findByCountryName(countryName)
                .orElseThrow(() -> new RuntimeException("Country not found: " + countryName));
    }

}
