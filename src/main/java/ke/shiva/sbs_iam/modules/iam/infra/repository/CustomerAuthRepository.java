package ke.shiva.sbs_iam.modules.iam.infra.repository;

import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAuthRepository extends JpaRepository<CustomerAuthEntity, UUID> {
    CustomerAuthEntity findByIamUser(@NotNull IamUserEntity iamUser);

    Optional<CustomerAuthEntity> findByIamUserId(Long id);
}
