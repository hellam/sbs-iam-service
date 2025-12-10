package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyScope;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "feature_policy", schema = "iam_service")
public class FeaturePolicyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 255)
    @Column(name = "description")
    private String description;

    @NotNull
    @Type(JsonType.class)
    @Column(name = "features", columnDefinition = "jsonb")
    private Set<Long> features;

    @NotNull
    @Column(name = "channel", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @NotNull
    @ColumnDefault("'GLOBAL'")
    @Column(name = "policy_scope", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PolicyScope policyScope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private PartyEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id")
    private IamUserEntity iamUser;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

}