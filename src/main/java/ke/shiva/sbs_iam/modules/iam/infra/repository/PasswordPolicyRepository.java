package ke.shiva.sbs_iam.modules.iam.infra.repository;

import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicyEntity, Long> {
    Optional<PasswordPolicyEntity> findFirstByChannel(@NotNull Channel channel);
}
