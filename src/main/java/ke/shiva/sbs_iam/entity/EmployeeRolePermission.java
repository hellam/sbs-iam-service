package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_role_permission", schema = "iam_service")
public class EmployeeRolePermission {
    @EmbeddedId
    private EmployeeRolePermissionId id;

    @MapsId("employeeRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_role_id", nullable = false)
    private EmployeeRole employeeRole;

    @MapsId("employeePermissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_permission_id", nullable = false)
    private EmployeePermission employeePermission;

    public EmployeeRolePermissionId getId() {
        return id;
    }

    public void setId(EmployeeRolePermissionId id) {
        this.id = id;
    }

    public EmployeeRole getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(EmployeeRole employeeRole) {
        this.employeeRole = employeeRole;
    }

    public EmployeePermission getEmployeePermission() {
        return employeePermission;
    }

    public void setEmployeePermission(EmployeePermission employeePermission) {
        this.employeePermission = employeePermission;
    }

}