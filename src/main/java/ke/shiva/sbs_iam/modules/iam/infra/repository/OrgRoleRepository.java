package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.client.iam.enums.TaskRole;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgRoleRepository extends JpaRepository<OrgRoleEntity, Long> {

    Optional<OrgRoleEntity> findByOrganizationPartyAndTaskRole(PartyEntity organizationParty, TaskRole taskRole);

    Optional<OrgRoleEntity> findByOrganizationPartyAndNameIgnoreCase(PartyEntity organizationParty, String name);

    List<OrgRoleEntity> findAllByOrganizationParty(PartyEntity organizationParty);
}
