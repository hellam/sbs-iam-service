package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PinPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PinPolicyRepository extends JpaRepository<PinPolicyEntity, Long> {
    Optional<PinPolicyEntity> findByPolicyIdAndChannel(Long policyId, Channel channel);
}
