package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "org_role", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class OrgRoleEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private PartyEntity organization;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @ColumnDefault("false")
    @Column(name = "is_default")
    private Boolean isDefault;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToMany(mappedBy = "orgRoles")
    private Set<FeatureEntity> features = new LinkedHashSet<>();

    @OneToMany(mappedBy = "orgRole")
    private Set<OrgUserRoleEntity> orgUserRoles = new LinkedHashSet<>();

    public PartyEntity getOrganization() {
        return organization;
    }

    public void setOrganization(PartyEntity organization) {
        this.organization = organization;
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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<FeatureEntity> getFeatures() {
        return features;
    }

    public void setFeatures(Set<FeatureEntity> features) {
        this.features = features;
    }

    public Set<OrgUserRoleEntity> getOrgUserRoles() {
        return orgUserRoles;
    }

    public void setOrgUserRoles(Set<OrgUserRoleEntity> orgUserRoles) {
        this.orgUserRoles = orgUserRoles;
    }

}