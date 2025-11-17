package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "org_user_role", schema = "iam_service")
public class OrgUserRole {
    @EmbeddedId
    private OrgUserRoleId id;

    @MapsId("organizationUserId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_user_id", nullable = false)
    private OrganizationUser organizationUser;

    @MapsId("orgRoleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_role_id", nullable = false)
    private OrgRole orgRole;

    @ColumnDefault("now()")
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    public OrgUserRoleId getId() {
        return id;
    }

    public void setId(OrgUserRoleId id) {
        this.id = id;
    }

    public OrganizationUser getOrganizationUser() {
        return organizationUser;
    }

    public void setOrganizationUser(OrganizationUser organizationUser) {
        this.organizationUser = organizationUser;
    }

    public OrgRole getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(OrgRole orgRole) {
        this.orgRole = orgRole;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

}