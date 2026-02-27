package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IamUserService {

    private final IamUserRepository iamUserRepository;
    private final UserContactRepository userContactRepository;


    /**
     * Get Iam User Primary Email and Phone Number by User ID.
     */

    public Map<String, String> getUserPrimaryContactInfo(IamUserEntity iamUser) {

        List<UserContact> userContact =userContactRepository.findByIamUserAndPrimaryIsTrue(iamUser)
                .orElseThrow(() -> new IllegalStateException("No primary contacts found for user: " + iamUser.getId()));
        String email = null;
        String phoneNumber = null;
        //loop and set by contact type
        for(UserContact contact : userContact){
            if(contact.getContactType().equals(ContactType.EMAIL)){
                email = contact.getContactValue();
            }
            if(contact.getContactType().equals(ContactType.PHONE)){
                phoneNumber = contact.getContactValue();
            }
        }

        assert email != null;
        assert phoneNumber != null;
        return Map.of(
                "email", email,
                "phone", phoneNumber
        );
    }

    public String getIamUserFullName(Long iamUserId){
        IamUserEntity user = iamUserRepository.findById(iamUserId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId={}", iamUserId);
                    return new RuntimeException("User not found");
                });

        return user.getParty() != null ? user.getParty().getPerson().getFullName() : "Unknown";
    }

}
