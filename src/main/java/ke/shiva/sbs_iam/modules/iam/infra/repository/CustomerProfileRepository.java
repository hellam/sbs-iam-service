package ke.shiva.sbs_iam.modules.iam.infra.repository;

import com.fasterxml.jackson.databind.introspect.AnnotationCollector;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, Long> {

    Optional<CustomerProfileEntity> findByIamUser(IamUserEntity iamUser);
    Optional<CustomerProfileEntity> findByIamUserAndIsVerifiedTrue(IamUserEntity iamUser);

    Optional<CustomerProfileEntity> findByCoreCustomerId(String coreCustomerId);
}
