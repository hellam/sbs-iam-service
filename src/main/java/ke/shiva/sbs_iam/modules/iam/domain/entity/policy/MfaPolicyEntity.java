package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import ke.shiva.sbs_iam.modules.iam.domain.enums.OtpType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.TransactionMfaMode;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "mfa_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class MfaPolicyEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private PolicyEntity policy;

    @NotNull
    @Column(name = "channel", length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_channels", columnDefinition = "jsonb")
    @ColumnDefault("'[\"SMS\"]'")
    private List<String> allowedNotificationChannels;

    @ColumnDefault("false")
    @Column(name = "allow_totp")
    private Boolean allowTotp;

    @ColumnDefault("3")
    @Column(name = "max_verify_attempts")
    private Short maxVerifyAttempts;

    @ColumnDefault("'NUMERIC'")
    @Column(name = "otp_type", length = 15)
    @Enumerated(EnumType.STRING)
    private OtpType otpType;

    @ColumnDefault("6")
    @Column(name = "otp_length")
    private Short otpLength;

    @ColumnDefault("'OTP'")
    @Column(name = "transaction_mfa_mode", length = 15)
    @Enumerated(EnumType.STRING)
    private TransactionMfaMode transactionMfaMode;

    @ColumnDefault("true")
    @Column(name = "enforce_on_transaction_initiation")
    private Boolean enforceOnTransactionInitiation;

    @ColumnDefault("true")
    @Column(name = "enforce_on_transaction_approval")
    private Boolean enforceOnTransactionApproval;

    @ColumnDefault("true")
    @Column(name = "enforce_on_transaction_rejection")
    private Boolean enforceOnTransactionRejection;

    @ColumnDefault("120")
    @Column(name = "otp_expiry_seconds")
    private Integer otpExpirySeconds;

    @ColumnDefault("10")
    @Column(name = "otp_daily_limit")
    private Short otpDailyLimit;

    @ColumnDefault("true")
    @Column(name = "enforce_on_new_device")
    private Boolean enforceOnNewDevice;

    @ColumnDefault("true")
    @Column(name = "enforce_on_new_location")
    private Boolean enforceOnNewLocation;

}
