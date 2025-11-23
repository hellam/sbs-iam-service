package ke.shiva.sbs_iam.modules.iam.infra.repository;

import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeAuthRepository extends JpaRepository<EmployeeAuthEntity, UUID> {
    Optional<EmployeeAuthEntity> findByIamUserId(Long id);

    EmployeeAuthEntity findByIamUser(IamUserEntity user);
}
