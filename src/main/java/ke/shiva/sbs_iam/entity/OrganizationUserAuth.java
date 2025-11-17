package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "organization_user_auth", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class OrganizationUserAuth extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_user_id", nullable = false)
    private OrganizationUser organizationUser;

    @Size(max = 255)
    @Column(name = "approver_password_hash")
    private String approverPasswordHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "approver_password_algo", length = 50)
    private String approverPasswordAlgo;

    @Size(max = 255)
    @Column(name = "approver_pin_hash")
    private String approverPinHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "approver_pin_algo", length = 50)
    private String approverPinAlgo;

    @Size(max = 255)
    @Column(name = "second_factor_secret")
    private String secondFactorSecret;

    @Column(name = "second_factor_last_verified_at")
    private OffsetDateTime secondFactorLastVerifiedAt;

    @ColumnDefault("0")
    @Column(name = "auth_approval_limit", precision = 18, scale = 2)
    private BigDecimal authApprovalLimit;

    public OrganizationUser getOrganizationUser() {
        return organizationUser;
    }

    public void setOrganizationUser(OrganizationUser organizationUser) {
        this.organizationUser = organizationUser;
    }

    public String getApproverPasswordHash() {
        return approverPasswordHash;
    }

    public void setApproverPasswordHash(String approverPasswordHash) {
        this.approverPasswordHash = approverPasswordHash;
    }

    public String getApproverPasswordAlgo() {
        return approverPasswordAlgo;
    }

    public void setApproverPasswordAlgo(String approverPasswordAlgo) {
        this.approverPasswordAlgo = approverPasswordAlgo;
    }

    public String getApproverPinHash() {
        return approverPinHash;
    }

    public void setApproverPinHash(String approverPinHash) {
        this.approverPinHash = approverPinHash;
    }

    public String getApproverPinAlgo() {
        return approverPinAlgo;
    }

    public void setApproverPinAlgo(String approverPinAlgo) {
        this.approverPinAlgo = approverPinAlgo;
    }

    public String getSecondFactorSecret() {
        return secondFactorSecret;
    }

    public void setSecondFactorSecret(String secondFactorSecret) {
        this.secondFactorSecret = secondFactorSecret;
    }

    public OffsetDateTime getSecondFactorLastVerifiedAt() {
        return secondFactorLastVerifiedAt;
    }

    public void setSecondFactorLastVerifiedAt(OffsetDateTime secondFactorLastVerifiedAt) {
        this.secondFactorLastVerifiedAt = secondFactorLastVerifiedAt;
    }

    public BigDecimal getAuthApprovalLimit() {
        return authApprovalLimit;
    }

    public void setAuthApprovalLimit(BigDecimal authApprovalLimit) {
        this.authApprovalLimit = authApprovalLimit;
    }

}