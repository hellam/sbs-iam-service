package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Setter
@Getter
@Entity
@Table(name = "pin_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class PinPolicyEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private PolicyEntity policy;

    @NotNull
    @Column(name = "channel", length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @ColumnDefault("4")
    @Column(name = "min_length")
    private Short minLength = (short) 4;

    @ColumnDefault("6")
    @Column(name = "max_length")
    private Short maxLength = (short) 6;

    @ColumnDefault("5")
    @Column(name = "pin_history_count")
    private Short pinHistoryCount = (short) 5;

    @ColumnDefault("true")
    @Column(name = "block_sequential")
    private Boolean blockSequential = true;

    @ColumnDefault("true")
    @Column(name = "block_repeating")
    private Boolean blockRepeating = true;

    @ColumnDefault("5")
    @Column(name = "max_failed_attempts")
    private Short maxFailedAttempts = (short) 5;

    @ColumnDefault("30")
    @Column(name = "lockout_minutes")
    private Short lockoutMinutes = (short) 30;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "hash_algorithm", length = 50)
    private String hashAlgorithm = "bcrypt";

    @ColumnDefault("10")
    @Column(name = "hash_cost")
    private Short hashCost = (short) 10;

}
