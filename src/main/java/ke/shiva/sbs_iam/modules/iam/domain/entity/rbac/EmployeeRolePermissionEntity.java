package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_role_permission", schema = "iam_service")
public class EmployeeRolePermissionEntity {
    @EmbeddedId
    private EmployeeRolePermissionIdEntity id;

    @MapsId("employeeRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_role_id", nullable = false)
    private EmployeeRoleEntity employeeRole;

    @MapsId("employeePermissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_permission_id", nullable = false)
    private EmployeePermissionEntity employeePermission;

    public EmployeeRolePermissionIdEntity getId() {
        return id;
    }

    public void setId(EmployeeRolePermissionIdEntity id) {
        this.id = id;
    }

    public EmployeeRoleEntity getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(EmployeeRoleEntity employeeRole) {
        this.employeeRole = employeeRole;
    }

    public EmployeePermissionEntity getEmployeePermission() {
        return employeePermission;
    }

    public void setEmployeePermission(EmployeePermissionEntity employeePermission) {
        this.employeePermission = employeePermission;
    }

}