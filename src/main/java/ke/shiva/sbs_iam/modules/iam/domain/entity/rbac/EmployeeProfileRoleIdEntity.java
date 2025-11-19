package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EmployeeProfileRoleIdEntity implements Serializable {
    private static final long serialVersionUID = 1432948776434479783L;
    @NotNull
    @Column(name = "employee_profile_iam_user_id", nullable = false)
    private Long employeeProfileIamUserId;

    @NotNull
    @Column(name = "employee_role_id", nullable = false)
    private Long employeeRoleId;

    public Long getEmployeeProfileIamUserId() {
        return employeeProfileIamUserId;
    }

    public void setEmployeeProfileIamUserId(Long employeeProfileIamUserId) {
        this.employeeProfileIamUserId = employeeProfileIamUserId;
    }

    public Long getEmployeeRoleId() {
        return employeeRoleId;
    }

    public void setEmployeeRoleId(Long employeeRoleId) {
        this.employeeRoleId = employeeRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        EmployeeProfileRoleIdEntity entity = (EmployeeProfileRoleIdEntity) o;
        return Objects.equals(this.employeeProfileIamUserId, entity.employeeProfileIamUserId) &&
                Objects.equals(this.employeeRoleId, entity.employeeRoleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeProfileIamUserId, employeeRoleId);
    }

}