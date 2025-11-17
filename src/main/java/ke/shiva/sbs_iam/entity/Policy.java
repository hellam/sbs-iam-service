package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

@Entity
@Table(name = "policies", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class Policy extends BaseEntity {
    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Size(max = 50)
    @NotNull
    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'GLOBAL'")
    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Party organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id")
    private IamUser iamUser;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @OneToOne(mappedBy = "policy")
    private MfaPolicy mfaPolicy;

    @OneToOne(mappedBy = "policy")
    private PasswordPolicy passwordPolicy;

    @OneToOne(mappedBy = "policy")
    private PinPolicy pinPolicy;

    @OneToOne(mappedBy = "policy")
    private SecurityQuestionPolicy securityQuestionPolicy;

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Party getOrganization() {
        return organization;
    }

    public void setOrganization(Party organization) {
        this.organization = organization;
    }

    public IamUser getIamUser() {
        return iamUser;
    }

    public void setIamUser(IamUser iamUser) {
        this.iamUser = iamUser;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public MfaPolicy getMfaPolicy() {
        return mfaPolicy;
    }

    public void setMfaPolicy(MfaPolicy mfaPolicy) {
        this.mfaPolicy = mfaPolicy;
    }

    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public PinPolicy getPinPolicy() {
        return pinPolicy;
    }

    public void setPinPolicy(PinPolicy pinPolicy) {
        this.pinPolicy = pinPolicy;
    }

    public SecurityQuestionPolicy getSecurityQuestionPolicy() {
        return securityQuestionPolicy;
    }

    public void setSecurityQuestionPolicy(SecurityQuestionPolicy securityQuestionPolicy) {
        this.securityQuestionPolicy = securityQuestionPolicy;
    }

}