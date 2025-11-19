package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OrgRolePermissionIdEntity implements Serializable {
    private static final long serialVersionUID = -4454228027447154161L;
    @NotNull
    @Column(name = "org_role_id", nullable = false)
    private Long orgRoleId;

    @NotNull
    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    public Long getOrgRoleId() {
        return orgRoleId;
    }

    public void setOrgRoleId(Long orgRoleId) {
        this.orgRoleId = orgRoleId;
    }

    public Long getFeatureId() {
        return featureId;
    }

    public void setFeatureId(Long featureId) {
        this.featureId = featureId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OrgRolePermissionIdEntity entity = (OrgRolePermissionIdEntity) o;
        return Objects.equals(this.orgRoleId, entity.orgRoleId) &&
                Objects.equals(this.featureId, entity.featureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgRoleId, featureId);
    }

}