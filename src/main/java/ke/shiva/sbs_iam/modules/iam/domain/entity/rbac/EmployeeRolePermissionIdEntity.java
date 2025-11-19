package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EmployeeRolePermissionIdEntity implements Serializable {
    private static final long serialVersionUID = -6659539292653400432L;
    @NotNull
    @Column(name = "employee_role_id", nullable = false)
    private Long employeeRoleId;

    @NotNull
    @Column(name = "employee_permission_id", nullable = false)
    private Long employeePermissionId;

    public Long getEmployeeRoleId() {
        return employeeRoleId;
    }

    public void setEmployeeRoleId(Long employeeRoleId) {
        this.employeeRoleId = employeeRoleId;
    }

    public Long getEmployeePermissionId() {
        return employeePermissionId;
    }

    public void setEmployeePermissionId(Long employeePermissionId) {
        this.employeePermissionId = employeePermissionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        EmployeeRolePermissionIdEntity entity = (EmployeeRolePermissionIdEntity) o;
        return Objects.equals(this.employeePermissionId, entity.employeePermissionId) &&
                Objects.equals(this.employeeRoleId, entity.employeeRoleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeePermissionId, employeeRoleId);
    }

}