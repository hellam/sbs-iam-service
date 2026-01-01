package ke.shiva.sbs_iam.modules.iam.domain.entity.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "employee_auth", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class EmployeeAuthEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

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

    @Column(name = "staff_lockout_until")
    private OffsetDateTime staffLockoutUntil;

    @ColumnDefault("true")
    @Column(name = "first_time_login", nullable = false)
    private Boolean firstTimeLogin = true;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Size(max = 255)
    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_last_verified_at")
    private OffsetDateTime mfaLastVerifiedAt;

}