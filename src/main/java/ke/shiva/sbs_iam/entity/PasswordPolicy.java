package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "password_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class PasswordPolicy extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ColumnDefault("12")
    @Column(name = "min_length")
    private Short minLength;

    @ColumnDefault("128")
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

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Short getMinLength() {
        return minLength;
    }

    public void setMinLength(Short minLength) {
        this.minLength = minLength;
    }

    public Short getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Short maxLength) {
        this.maxLength = maxLength;
    }

    public Boolean getRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(Boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public Boolean getRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(Boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public Boolean getRequireNumber() {
        return requireNumber;
    }

    public void setRequireNumber(Boolean requireNumber) {
        this.requireNumber = requireNumber;
    }

    public Boolean getRequireSymbol() {
        return requireSymbol;
    }

    public void setRequireSymbol(Boolean requireSymbol) {
        this.requireSymbol = requireSymbol;
    }

    public Boolean getBlockCommonPasswords() {
        return blockCommonPasswords;
    }

    public void setBlockCommonPasswords(Boolean blockCommonPasswords) {
        this.blockCommonPasswords = blockCommonPasswords;
    }

    public Short getPasswordHistoryCount() {
        return passwordHistoryCount;
    }

    public void setPasswordHistoryCount(Short passwordHistoryCount) {
        this.passwordHistoryCount = passwordHistoryCount;
    }

    public Boolean getExpirationEnabled() {
        return expirationEnabled;
    }

    public void setExpirationEnabled(Boolean expirationEnabled) {
        this.expirationEnabled = expirationEnabled;
    }

    public Short getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(Short expirationDays) {
        this.expirationDays = expirationDays;
    }

    public Short getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(Short maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public Short getLockoutMinutes() {
        return lockoutMinutes;
    }

    public void setLockoutMinutes(Short lockoutMinutes) {
        this.lockoutMinutes = lockoutMinutes;
    }

    public Boolean getRequireFactoryReset() {
        return requireFactoryReset;
    }

    public void setRequireFactoryReset(Boolean requireFactoryReset) {
        this.requireFactoryReset = requireFactoryReset;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public Short getHashCost() {
        return hashCost;
    }

    public void setHashCost(Short hashCost) {
        this.hashCost = hashCost;
    }

}