package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SecurityQuestionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityQuestionPolicyRepository extends JpaRepository<SecurityQuestionPolicyEntity, Long> {
    SecurityQuestionPolicyEntity findByChannel(Channel channel);
    Optional<SecurityQuestionPolicyEntity> findByPolicyIdAndChannel(Long policyId, Channel channel);
}
