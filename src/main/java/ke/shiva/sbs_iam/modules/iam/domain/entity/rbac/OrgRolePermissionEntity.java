package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;

@Entity
@Table(name = "org_role_permission", schema = "iam_service")
public class OrgRolePermissionEntity {
    @EmbeddedId
    private OrgRolePermissionIdEntity id;

    @MapsId("orgRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_role_id", nullable = false)
    private OrgRoleEntity orgRole;

    @MapsId("featureId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private FeatureEntity feature;

    public OrgRolePermissionIdEntity getId() {
        return id;
    }

    public void setId(OrgRolePermissionIdEntity id) {
        this.id = id;
    }

    public OrgRoleEntity getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(OrgRoleEntity orgRole) {
        this.orgRole = orgRole;
    }

    public FeatureEntity getFeature() {
        return feature;
    }

    public void setFeature(FeatureEntity feature) {
        this.feature = feature;
    }

}