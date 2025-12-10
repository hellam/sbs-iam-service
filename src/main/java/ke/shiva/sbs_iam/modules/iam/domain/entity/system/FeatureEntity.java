package ke.shiva.sbs_iam.modules.iam.domain.entity.system;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.FeaturePolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "features", schema = "iam_service")
public class FeatureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "channel", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Size(max = 100)
    @NotNull
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

}