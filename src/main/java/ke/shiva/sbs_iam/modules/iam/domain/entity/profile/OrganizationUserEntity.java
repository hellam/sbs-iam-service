package ke.shiva.sbs_iam.modules.iam.domain.entity.profile;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgUserRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.OrganizationApproverSecretHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.OrganizationUserAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "organization_user", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class OrganizationUserEntity extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_party_id", nullable = false)
    private PartyEntity organizationParty;

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
    private Set<OrgUserRoleEntity> orgUserRoles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "organizationUser")
    private Set<OrganizationApproverSecretHistoryEntity> organizationApproverSecretHistories = new LinkedHashSet<>();

    @OneToOne(mappedBy = "organizationUser")
    private OrganizationUserAuthEntity organizationUserAuth;


    public String getOrgDisplayName() {
        return organizationParty.getOrganization().getDisplayName();
    }
}