package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IamUserRepository extends JpaRepository<IamUserEntity, Long> {
    Optional<IamUserEntity> findFirstByParty_CoreCustomerId(String coreCustomerId);

    Optional<IamUserEntity> findFirstByParty_Person_NationalIdIgnoreCase(String nationalId);
}
