package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.FeaturePolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeaturePolicyRepository extends JpaRepository<FeaturePolicyEntity, Long> {
    List<FeaturePolicyEntity> findByChannelAndPolicyScopeAndIsActiveTrue(Channel channel, PolicyScope policyScope);
    Optional<FeaturePolicyEntity> findByNameAndChannelAndPolicyScope(String name, Channel channel, PolicyScope policyScope);
}
