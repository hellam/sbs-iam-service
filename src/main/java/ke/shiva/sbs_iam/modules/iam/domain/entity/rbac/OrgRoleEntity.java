package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.client.iam.enums.TaskRole;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "org_role", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class OrgRoleEntity extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_party_id", nullable = false)
    private PartyEntity organizationParty;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "task_role", nullable = false, length = 30)
    private TaskRole taskRole;

    @ColumnDefault("false")
    @Column(name = "is_default")
    private Boolean isDefault;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "orgRole")
    private Set<OrganizationUserEntity> organizationUsers = new LinkedHashSet<>();

}
