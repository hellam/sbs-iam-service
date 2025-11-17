package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "employee_profile_role", schema = "iam_service")
public class EmployeeProfileRole {
    @EmbeddedId
    private EmployeeProfileRoleId id;

    @MapsId("employeeProfileIamUserId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_profile_iam_user_id", nullable = false)
    private EmployeeProfile employeeProfileIamUser;

    @MapsId("employeeRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_role_id", nullable = false)
    private EmployeeRole employeeRole;

    @ColumnDefault("now()")
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    public EmployeeProfileRoleId getId() {
        return id;
    }

    public void setId(EmployeeProfileRoleId id) {
        this.id = id;
    }

    public EmployeeProfile getEmployeeProfileIamUser() {
        return employeeProfileIamUser;
    }

    public void setEmployeeProfileIamUser(EmployeeProfile employeeProfileIamUser) {
        this.employeeProfileIamUser = employeeProfileIamUser;
    }

    public EmployeeRole getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(EmployeeRole employeeRole) {
        this.employeeRole = employeeRole;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

}