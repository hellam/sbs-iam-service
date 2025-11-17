package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "employee_role", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class EmployeeRole extends BaseEntity {
    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @ColumnDefault("false")
    @Column(name = "is_system")
    private Boolean isSystem;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "employeeRole")
    private Set<EmployeeProfileRole> employeeProfileRoles = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "employeeRoles")
    private Set<EmployeePermission> employeePermissions = new LinkedHashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<EmployeeProfileRole> getEmployeeProfileRoles() {
        return employeeProfileRoles;
    }

    public void setEmployeeProfileRoles(Set<EmployeeProfileRole> employeeProfileRoles) {
        this.employeeProfileRoles = employeeProfileRoles;
    }

    public Set<EmployeePermission> getEmployeePermissions() {
        return employeePermissions;
    }

    public void setEmployeePermissions(Set<EmployeePermission> employeePermissions) {
        this.employeePermissions = employeePermissions;
    }

}