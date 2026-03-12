package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionIdEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrgRolePermissionRepository extends JpaRepository<OrgRolePermissionEntity, OrgRolePermissionIdEntity> {
    @EntityGraph(attributePaths = {"orgRole", "feature"})
    List<OrgRolePermissionEntity> findByOrgRole_OrganizationParty(PartyEntity organizationParty);

    @EntityGraph(attributePaths = {"orgRole", "feature"})
    List<OrgRolePermissionEntity> findByOrgRole(OrgRoleEntity orgRole);
}
