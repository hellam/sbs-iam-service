package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "org_role_permission", schema = "iam_service")
public class OrgRolePermission {
    @EmbeddedId
    private OrgRolePermissionId id;

    @MapsId("orgRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_role_id", nullable = false)
    private OrgRole orgRole;

    @MapsId("featureId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;

    public OrgRolePermissionId getId() {
        return id;
    }

    public void setId(OrgRolePermissionId id) {
        this.id = id;
    }

    public OrgRole getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(OrgRole orgRole) {
        this.orgRole = orgRole;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

}