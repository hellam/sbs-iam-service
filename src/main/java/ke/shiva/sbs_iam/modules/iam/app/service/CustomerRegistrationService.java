package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.client.iam.dto.request.CustomerRegistrationDetailsRequest;
import ke.shiva.client.iam.dto.request.CustomerRegistrationValidationRequest;
import ke.shiva.client.iam.dto.response.CustomerRegistrationResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
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
    private final PasswordPolicyService passwordPolicyService;

    @Transactional
    public CustomerRegistrationResponse registerCustomer(CustomerRegistrationDetailsRequest request) {
        /* -------------------------------------------------
         * 0. We check if a customer profile, Person, or Contact already exists with the given details
         * -------------------------------------------------- */
        validateInternetCustomer(CustomerRegistrationValidationRequest.builder()
                .clientId(request.getClientId()).nationalId(request.getNationalId())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .build());

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
        person.setFullName(request.getFirstName() + " " + request.getMiddleName() + " " + request.getLastName());
        person.setNationalId(request.getNationalId());
        person.setCountryCode(getCountryCode(request.getCountry()));
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
        String uniqueUsername = UsernameGeneratorUtil.generateUniqueNumericUsername(8,
                username -> loginIdentifierRepository.existsByChannelAndIdentifierTypeAndIdentifier(
                        Channel.INTERNET_BANKING, "username", username));

        LoginIdentifierEntity loginIdentifier = new LoginIdentifierEntity();
        loginIdentifier.setIamUser(iamUser);
        loginIdentifier.setChannel(Channel.INTERNET_BANKING);
        loginIdentifier.setIdentifierType("username");
        loginIdentifier.setIdentifier(uniqueUsername);
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
        customerProfile.setIsVerified(true);
        customerProfile.setAllowEmail(true);
        customerProfile.setAllowSms(true);
        customerProfile.setAllowPush(false);
        customerProfile.setCreatedAt(LocalDateTime.now());
        customerProfile.setUpdatedAt(LocalDateTime.now());
        customerProfileRepository.save(customerProfile);

        /* -------------------------------------------------
         * 8. Create Customer Auth
         * ------------------------------------------------- */
        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(Channel.INTERNET_BANKING);
        int passwordLength = (policy != null && policy.getMinLength() != null) ? policy.getMinLength() : 8;
        String rawPassword = PasswordGeneratorUtil.generateNumericPassword(passwordLength);

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

        log.info("Full IAM customer registration completed for clientId={}, iamUserId={}",
                request.getClientId(), iamUser.getId());

        /* -------------------------------------------------
         * 9. Return Registration Response
         * ------------------------------------------------- */
        return CustomerRegistrationResponse.builder()
                .iamUserId(iamUser.getId())
                .username(uniqueUsername)
                .name(request.getFirstName())
                .generatedPassword(rawPassword)
                .build();
    }

    public void validateInternetCustomer(CustomerRegistrationValidationRequest request) {
        if (customerProfileRepository.findByCoreCustomerId(request.getClientId()).isPresent()) {
            throw BaseException.badRequest("Client ID '" + request.getClientId() + "' is already registered.");
        }

        if (personRepository.existsByNationalId(request.getNationalId()))
            throw BaseException.badRequest("National ID '" + request.getNationalId() + "' is already registered.");

        //check if phone exists
        String mobileNo = request.getMobile().substring(request.getMobile().length() - 9);
        if (userContactRepository.existsByContactTypeAndContactValueContaining(ContactType.PHONE, mobileNo))
            throw BaseException.badRequest("Phone number '" + request.getMobile() + "' is already registered.");

        //check if email exists
        if (request.getEmail() != null)
            if (userContactRepository.existsByContactTypeAndContactValue(ContactType.EMAIL, request.getEmail()))
                throw BaseException.badRequest("Email '" + request.getEmail() + "' is already registered.");
    }

    private CountryEntity getCountryCode(String countryName) {
        return countryRepository.findByCountryNameIgnoreCase(countryName.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Country not found: " + countryName));
    }
}
