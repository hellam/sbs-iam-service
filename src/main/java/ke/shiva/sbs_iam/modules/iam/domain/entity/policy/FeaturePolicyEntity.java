package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "feature_policy", schema = "iam_service")
public class FeaturePolicyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private FeatureEntity feature;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "featurePolicy")
    private Set<FeaturePolicyAssignmentEntity> featurePolicyAssignments = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeatureEntity getFeature() {
        return feature;
    }

    public void setFeature(FeatureEntity feature) {
        this.feature = feature;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<FeaturePolicyAssignmentEntity> getFeaturePolicyAssignments() {
        return featurePolicyAssignments;
    }

    public void setFeaturePolicyAssignments(Set<FeaturePolicyAssignmentEntity> featurePolicyAssignments) {
        this.featurePolicyAssignments = featurePolicyAssignments;
    }

}