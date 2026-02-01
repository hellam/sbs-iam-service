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
@Table(name = "password_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class PasswordPolicyEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private PolicyEntity policy;

    @NotNull
    @Column(name = "channel", length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @ColumnDefault("8")
    @Column(name = "min_length")
    private Short minLength;

    @ColumnDefault("32")
    @Column(name = "max_length")
    private Short maxLength;

    @ColumnDefault("false")
    @Column(name = "require_uppercase")
    private Boolean requireUppercase;

    @ColumnDefault("false")
    @Column(name = "require_lowercase")
    private Boolean requireLowercase;

    @ColumnDefault("false")
    @Column(name = "require_number")
    private Boolean requireNumber;

    @ColumnDefault("false")
    @Column(name = "require_symbol")
    private Boolean requireSymbol;

    @ColumnDefault("true")
    @Column(name = "block_common_passwords")
    private Boolean blockCommonPasswords;

    @ColumnDefault("5")
    @Column(name = "password_history_count")
    private Short passwordHistoryCount;

    @ColumnDefault("false")
    @Column(name = "expiration_enabled")
    private Boolean expirationEnabled;

    @ColumnDefault("90")
    @Column(name = "expiration_days")
    private Short expirationDays;

    @ColumnDefault("5")
    @Column(name = "max_failed_attempts")
    private Short maxFailedAttempts;

    @ColumnDefault("30")
    @Column(name = "lockout_minutes")
    private Short lockoutMinutes;

    @ColumnDefault("false")
    @Column(name = "require_factory_reset")
    private Boolean requireFactoryReset;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "hash_algorithm", length = 50)
    private String hashAlgorithm;

    @ColumnDefault("12")
    @Column(name = "hash_cost")
    private Short hashCost;
}