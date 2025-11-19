package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "feature_policy_assignment", schema = "iam_service")
public class FeaturePolicyAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_policy_id", nullable = false)
    private FeaturePolicyEntity featurePolicy;

    @Size(max = 50)
    @ColumnDefault("'GLOBAL'")
    @Column(name = "scope", length = 50)
    private String scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private PartyEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id")
    private IamUserEntity iamUser;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeaturePolicyEntity getFeaturePolicy() {
        return featurePolicy;
    }

    public void setFeaturePolicy(FeaturePolicyEntity featurePolicy) {
        this.featurePolicy = featurePolicy;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public PartyEntity getOrganization() {
        return organization;
    }

    public void setOrganization(PartyEntity organization) {
        this.organization = organization;
    }

    public IamUserEntity getIamUser() {
        return iamUser;
    }

    public void setIamUser(IamUserEntity iamUser) {
        this.iamUser = iamUser;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

}