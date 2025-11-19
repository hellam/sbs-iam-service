package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "employee_profile_role", schema = "iam_service")
public class EmployeeProfileRoleEntity {
    @EmbeddedId
    private EmployeeProfileRoleIdEntity id;

    @MapsId("employeeProfileIamUserId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_profile_iam_user_id", nullable = false)
    private EmployeeProfileEntity employeeProfileIamUser;

    @MapsId("employeeRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_role_id", nullable = false)
    private EmployeeRoleEntity employeeRole;

    @ColumnDefault("now()")
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    public EmployeeProfileRoleIdEntity getId() {
        return id;
    }

    public void setId(EmployeeProfileRoleIdEntity id) {
        this.id = id;
    }

    public EmployeeProfileEntity getEmployeeProfileIamUser() {
        return employeeProfileIamUser;
    }

    public void setEmployeeProfileIamUser(EmployeeProfileEntity employeeProfileIamUser) {
        this.employeeProfileIamUser = employeeProfileIamUser;
    }

    public EmployeeRoleEntity getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(EmployeeRoleEntity employeeRole) {
        this.employeeRole = employeeRole;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

}