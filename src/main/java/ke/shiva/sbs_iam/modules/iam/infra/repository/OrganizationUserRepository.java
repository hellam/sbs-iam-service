package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUserEntity, Long> {

    @EntityGraph(attributePaths = {"organizationParty", "organizationParty.organization"})
    List<OrganizationUserEntity> findAllByIamUser(IamUserEntity iamUser);

    Optional<OrganizationUserEntity> findByIamUserAndOrganizationParty(IamUserEntity iamUser, PartyEntity organizationParty);
}
