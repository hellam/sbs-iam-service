package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SessionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionPolicyRepository extends JpaRepository<SessionPolicyEntity, Long> {
    SessionPolicyEntity findByChannel(Channel channel);
    Optional<SessionPolicyEntity> findByPolicyIdAndChannel(Long policyId, Channel channel);
}
