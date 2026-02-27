package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRolePermissionIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgRolePermissionRepository extends JpaRepository<OrgRolePermissionEntity, OrgRolePermissionIdEntity> {
}
