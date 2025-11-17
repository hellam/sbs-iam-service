package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Entity
@Table(name = "mfa_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class MfaPolicy extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

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

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Boolean getRequireMfaIb() {
        return requireMfaIb;
    }

    public void setRequireMfaIb(Boolean requireMfaIb) {
        this.requireMfaIb = requireMfaIb;
    }

    public Boolean getRequireMfaMb() {
        return requireMfaMb;
    }

    public void setRequireMfaMb(Boolean requireMfaMb) {
        this.requireMfaMb = requireMfaMb;
    }

    public Boolean getRequireMfaBackoffice() {
        return requireMfaBackoffice;
    }

    public void setRequireMfaBackoffice(Boolean requireMfaBackoffice) {
        this.requireMfaBackoffice = requireMfaBackoffice;
    }

    public Boolean getAllowTotp() {
        return allowTotp;
    }

    public void setAllowTotp(Boolean allowTotp) {
        this.allowTotp = allowTotp;
    }

    public Boolean getAllowSmsOtp() {
        return allowSmsOtp;
    }

    public void setAllowSmsOtp(Boolean allowSmsOtp) {
        this.allowSmsOtp = allowSmsOtp;
    }

    public Boolean getAllowEmailOtp() {
        return allowEmailOtp;
    }

    public void setAllowEmailOtp(Boolean allowEmailOtp) {
        this.allowEmailOtp = allowEmailOtp;
    }

    public Boolean getAllowWhatsappOtp() {
        return allowWhatsappOtp;
    }

    public void setAllowWhatsappOtp(Boolean allowWhatsappOtp) {
        this.allowWhatsappOtp = allowWhatsappOtp;
    }

    public Boolean getAllowPush() {
        return allowPush;
    }

    public void setAllowPush(Boolean allowPush) {
        this.allowPush = allowPush;
    }

    public Boolean getAllowWebauthn() {
        return allowWebauthn;
    }

    public void setAllowWebauthn(Boolean allowWebauthn) {
        this.allowWebauthn = allowWebauthn;
    }

    public Integer getOtpExpirySeconds() {
        return otpExpirySeconds;
    }

    public void setOtpExpirySeconds(Integer otpExpirySeconds) {
        this.otpExpirySeconds = otpExpirySeconds;
    }

    public Short getOtpDailyLimit() {
        return otpDailyLimit;
    }

    public void setOtpDailyLimit(Short otpDailyLimit) {
        this.otpDailyLimit = otpDailyLimit;
    }

    public Boolean getRequireMfaHighValueTxn() {
        return requireMfaHighValueTxn;
    }

    public void setRequireMfaHighValueTxn(Boolean requireMfaHighValueTxn) {
        this.requireMfaHighValueTxn = requireMfaHighValueTxn;
    }

    public BigDecimal getHighValueThreshold() {
        return highValueThreshold;
    }

    public void setHighValueThreshold(BigDecimal highValueThreshold) {
        this.highValueThreshold = highValueThreshold;
    }

    public Boolean getEnforceOnNewDevice() {
        return enforceOnNewDevice;
    }

    public void setEnforceOnNewDevice(Boolean enforceOnNewDevice) {
        this.enforceOnNewDevice = enforceOnNewDevice;
    }

    public Boolean getEnforceOnNewLocation() {
        return enforceOnNewLocation;
    }

    public void setEnforceOnNewLocation(Boolean enforceOnNewLocation) {
        this.enforceOnNewLocation = enforceOnNewLocation;
    }

}