package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "employee_permission", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class EmployeePermissionEntity extends BaseEntity {
    @Size(max = 100)
    @NotNull
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToMany(mappedBy = "employeePermissions")
    private Set<EmployeeRoleEntity> employeeRoles = new LinkedHashSet<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<EmployeeRoleEntity> getEmployeeRoles() {
        return employeeRoles;
    }

    public void setEmployeeRoles(Set<EmployeeRoleEntity> employeeRoles) {
        this.employeeRoles = employeeRoles;
    }

}