package ke.shiva.sbs_iam.modules.iam.domain.entity.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OrgUserRoleIdEntity implements Serializable {
    private static final long serialVersionUID = 1984510198618854613L;
    @NotNull
    @Column(name = "organization_user_id", nullable = false)
    private Long organizationUserId;

    @NotNull
    @Column(name = "org_role_id", nullable = false)
    private Long orgRoleId;

    public Long getOrganizationUserId() {
        return organizationUserId;
    }

    public void setOrganizationUserId(Long organizationUserId) {
        this.organizationUserId = organizationUserId;
    }

    public Long getOrgRoleId() {
        return orgRoleId;
    }

    public void setOrgRoleId(Long orgRoleId) {
        this.orgRoleId = orgRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OrgUserRoleIdEntity entity = (OrgUserRoleIdEntity) o;
        return Objects.equals(this.organizationUserId, entity.organizationUserId) &&
                Objects.equals(this.orgRoleId, entity.orgRoleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationUserId, orgRoleId);
    }

}