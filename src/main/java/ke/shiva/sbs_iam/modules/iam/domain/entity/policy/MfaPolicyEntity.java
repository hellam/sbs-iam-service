package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

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

    @ColumnDefault("true")
    @Column(name = "require_mfa_ib")
    private Boolean requireMfaIb;

    @ColumnDefault("false")
    @Column(name = "require_mfa_mb")
    private Boolean requireMfaMb;

    @ColumnDefault("true")
    @Column(name = "require_mfa_backoffice")
    private Boolean requireMfaBackoffice;

    @ColumnDefault("true")
    @Column(name = "allow_totp")
    private Boolean allowTotp;

    @ColumnDefault("true")
    @Column(name = "allow_sms_otp")
    private Boolean allowSmsOtp;

    @ColumnDefault("true")
    @Column(name = "allow_email_otp")
    private Boolean allowEmailOtp;

    @ColumnDefault("true")
    @Column(name = "allow_whatsapp_otp")
    private Boolean allowWhatsappOtp;

    @ColumnDefault("false")
    @Column(name = "allow_push")
    private Boolean allowPush;

    @ColumnDefault("false")
    @Column(name = "allow_webauthn")
    private Boolean allowWebauthn;

    @ColumnDefault("120")
    @Column(name = "otp_expiry_seconds")
    private Integer otpExpirySeconds;

    @ColumnDefault("10")
    @Column(name = "otp_daily_limit")
    private Short otpDailyLimit;

    @ColumnDefault("true")
    @Column(name = "require_mfa_high_value_txn")
    private Boolean requireMfaHighValueTxn;

    @ColumnDefault("5000")
    @Column(name = "high_value_threshold", precision = 18, scale = 2)
    private BigDecimal highValueThreshold;

    @ColumnDefault("true")
    @Column(name = "enforce_on_new_device")
    private Boolean enforceOnNewDevice;

    @ColumnDefault("true")
    @Column(name = "enforce_on_new_location")
    private Boolean enforceOnNewLocation;

}