package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.EmployeeProfileRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.EmployeeProfileRoleIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeProfileRoleRepository extends JpaRepository<EmployeeProfileRoleEntity, EmployeeProfileRoleIdEntity> {
}
