package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "employee_auth", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class EmployeeAuth extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUser iamUser;

    @Size(max = 255)
    @Column(name = "staff_password_hash")
    private String staffPasswordHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "staff_password_algo", length = 50)
    private String staffPasswordAlgo;

    @Column(name = "staff_password_expiry")
    private OffsetDateTime staffPasswordExpiry;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "staff_failed_attempts", nullable = false)
    private Short staffFailedAttempts;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "staff_locked", nullable = false)
    private Boolean staffLocked = false;

    @Column(name = "staff_last_login_at")
    private OffsetDateTime staffLastLoginAt;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Size(max = 255)
    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_last_verified_at")
    private OffsetDateTime mfaLastVerifiedAt;

    public IamUser getIamUser() {
        return iamUser;
    }

    public void setIamUser(IamUser iamUser) {
        this.iamUser = iamUser;
    }

    public String getStaffPasswordHash() {
        return staffPasswordHash;
    }

    public void setStaffPasswordHash(String staffPasswordHash) {
        this.staffPasswordHash = staffPasswordHash;
    }

    public String getStaffPasswordAlgo() {
        return staffPasswordAlgo;
    }

    public void setStaffPasswordAlgo(String staffPasswordAlgo) {
        this.staffPasswordAlgo = staffPasswordAlgo;
    }

    public OffsetDateTime getStaffPasswordExpiry() {
        return staffPasswordExpiry;
    }

    public void setStaffPasswordExpiry(OffsetDateTime staffPasswordExpiry) {
        this.staffPasswordExpiry = staffPasswordExpiry;
    }

    public Short getStaffFailedAttempts() {
        return staffFailedAttempts;
    }

    public void setStaffFailedAttempts(Short staffFailedAttempts) {
        this.staffFailedAttempts = staffFailedAttempts;
    }

    public Boolean getStaffLocked() {
        return staffLocked;
    }

    public void setStaffLocked(Boolean staffLocked) {
        this.staffLocked = staffLocked;
    }

    public OffsetDateTime getStaffLastLoginAt() {
        return staffLastLoginAt;
    }

    public void setStaffLastLoginAt(OffsetDateTime staffLastLoginAt) {
        this.staffLastLoginAt = staffLastLoginAt;
    }

    public Boolean getMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public OffsetDateTime getMfaLastVerifiedAt() {
        return mfaLastVerifiedAt;
    }

    public void setMfaLastVerifiedAt(OffsetDateTime mfaLastVerifiedAt) {
        this.mfaLastVerifiedAt = mfaLastVerifiedAt;
    }

}