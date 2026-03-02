package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, Long>,
        JpaSpecificationExecutor<CustomerProfileEntity> {

    Optional<CustomerProfileEntity> findByIamUser(IamUserEntity iamUser);
    Optional<CustomerProfileEntity> findByIamUserAndIsVerifiedTrue(IamUserEntity iamUser);

    Optional<CustomerProfileEntity> findByCoreCustomerId(String coreCustomerId);
}
