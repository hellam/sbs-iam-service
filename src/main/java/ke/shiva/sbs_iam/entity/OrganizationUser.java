package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "organization_user", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class OrganizationUser extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUser iamUser;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_party_id", nullable = false)
    private Party organizationParty;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'VIEW_ONLY'")
    @Column(name = "org_role", nullable = false, length = 20)
    private String orgRole;

    @Column(name = "approval_limit", precision = 18, scale = 2)
    private BigDecimal approvalLimit;

    @ColumnDefault("false")
    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @OneToMany(mappedBy = "organizationUser")
    private Set<OrgUserRole> orgUserRoles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "organizationUser")
    private Set<OrganizationApproverSecretHistory> organizationApproverSecretHistories = new LinkedHashSet<>();

    @OneToOne(mappedBy = "organizationUser")
    private OrganizationUserAuth organizationUserAuth;

    public IamUser getIamUser() {
        return iamUser;
    }

    public void setIamUser(IamUser iamUser) {
        this.iamUser = iamUser;
    }

    public Party getOrganizationParty() {
        return organizationParty;
    }

    public void setOrganizationParty(Party organizationParty) {
        this.organizationParty = organizationParty;
    }

    public String getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(String orgRole) {
        this.orgRole = orgRole;
    }

    public BigDecimal getApprovalLimit() {
        return approvalLimit;
    }

    public void setApprovalLimit(BigDecimal approvalLimit) {
        this.approvalLimit = approvalLimit;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<OrgUserRole> getOrgUserRoles() {
        return orgUserRoles;
    }

    public void setOrgUserRoles(Set<OrgUserRole> orgUserRoles) {
        this.orgUserRoles = orgUserRoles;
    }

    public Set<OrganizationApproverSecretHistory> getOrganizationApproverSecretHistories() {
        return organizationApproverSecretHistories;
    }

    public void setOrganizationApproverSecretHistories(Set<OrganizationApproverSecretHistory> organizationApproverSecretHistories) {
        this.organizationApproverSecretHistories = organizationApproverSecretHistories;
    }

    public OrganizationUserAuth getOrganizationUserAuth() {
        return organizationUserAuth;
    }

    public void setOrganizationUserAuth(OrganizationUserAuth organizationUserAuth) {
        this.organizationUserAuth = organizationUserAuth;
    }

}