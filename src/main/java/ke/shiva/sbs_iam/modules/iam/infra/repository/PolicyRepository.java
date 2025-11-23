package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<PolicyEntity, Long> {
    Optional<PolicyEntity> findByIamUserAndScopeAndIsActiveTrue(IamUserEntity iamUser, PolicyScope scope);
    Optional<PolicyEntity> findByOrganizationAndScopeAndIsActiveTrue(PartyEntity organization, PolicyScope scope);
    Optional<PolicyEntity> findFirstByScopeAndIsActiveTrue(PolicyScope scope);
}