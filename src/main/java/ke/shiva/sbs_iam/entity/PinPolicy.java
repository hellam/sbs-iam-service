package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "pin_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class PinPolicy extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ColumnDefault("4")
    @Column(name = "min_length")
    private Short minLength;

    @ColumnDefault("6")
    @Column(name = "max_length")
    private Short maxLength;

    @ColumnDefault("5")
    @Column(name = "pin_history_count")
    private Short pinHistoryCount;

    @ColumnDefault("true")
    @Column(name = "block_sequential")
    private Boolean blockSequential;

    @ColumnDefault("true")
    @Column(name = "block_repeating")
    private Boolean blockRepeating;

    @ColumnDefault("5")
    @Column(name = "max_failed_attempts")
    private Short maxFailedAttempts;

    @ColumnDefault("30")
    @Column(name = "lockout_minutes")
    private Short lockoutMinutes;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "hash_algorithm", length = 50)
    private String hashAlgorithm;

    @ColumnDefault("10")
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

    public Short getPinHistoryCount() {
        return pinHistoryCount;
    }

    public void setPinHistoryCount(Short pinHistoryCount) {
        this.pinHistoryCount = pinHistoryCount;
    }

    public Boolean getBlockSequential() {
        return blockSequential;
    }

    public void setBlockSequential(Boolean blockSequential) {
        this.blockSequential = blockSequential;
    }

    public Boolean getBlockRepeating() {
        return blockRepeating;
    }

    public void setBlockRepeating(Boolean blockRepeating) {
        this.blockRepeating = blockRepeating;
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