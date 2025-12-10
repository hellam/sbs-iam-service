package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<PolicyEntity, Long> {
    @Query(value = "SELECT * FROM iam_service.policies WHERE jsonb_exists(channels, :channel) LIMIT 1", nativeQuery = true)
    Optional<PolicyEntity> findFirstByChannelsContains(@Param("channel") String channel);

    Optional<PolicyEntity> findByPolicyType(PolicyType policyType);

    @Query(value = "SELECT * FROM iam_service.policies WHERE policy_type = :#{#policyType.name()} AND jsonb_exists(channels, :channel)", nativeQuery = true)
    List<PolicyEntity> findByPolicyTypeAndChannelsContaining(@Param("policyType") PolicyType policyType, @Param("channel") String channel);
}

