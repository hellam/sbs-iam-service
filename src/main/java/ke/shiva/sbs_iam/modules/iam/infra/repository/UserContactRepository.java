package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserContactRepository extends JpaRepository<UserContact, Long> {
    Optional<UserContact> findByIamUserAndContactTypeAndPrimaryIsTrue(IamUserEntity iamUser, ContactType contactType);

    Optional<List<UserContact>> findByIamUserAndPrimaryIsTrue(IamUserEntity iamUser);

    boolean existsByContactTypeAndContactValueLike(ContactType contactType, String contactValue);

    boolean existsByContactTypeAndContactValue(ContactType contactType, String contactValue);

    boolean existsByContactTypeAndContactValueContaining(ContactType contactType, String contactValue);
}

