package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Setter
@Getter
@Entity
@Table(name = "session_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class SessionPolicyEntity extends BaseEntity {

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private PolicyEntity policy;

    @NotNull
    @Column(name = "channel", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @NotNull
    @ColumnDefault("180")
    @Column(name = "inactivity_timeout_seconds", nullable = false)
    private Integer inactivityTimeoutSeconds = 180;

    @NotNull
    @ColumnDefault("60")
    @Column(name = "warning_countdown_seconds", nullable = false)
    private Integer warningCountdownSeconds = 60;
}
