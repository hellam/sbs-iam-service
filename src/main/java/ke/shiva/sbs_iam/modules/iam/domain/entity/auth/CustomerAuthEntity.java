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
@Table(name = "customer_auth", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class CustomerAuthEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @Size(max = 255)
    @Column(name = "internet_password_hash")
    private String internetPasswordHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "internet_password_algo", length = 50)
    private String internetPasswordAlgo;

    @Column(name = "internet_password_expiry")
    private OffsetDateTime internetPasswordExpiry;

    @Column(name = "internet_password_changed_at")
    private OffsetDateTime internetPasswordChangedAt;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "internet_first_time_login", nullable = false)
    private Boolean internetFirstTimeLogin = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "internet_failed_attempts", nullable = false)
    private Short internetFailedAttempts;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "internet_locked", nullable = false)
    private Boolean internetLocked = false;

    @Column(name = "internet_last_login_at")
    private OffsetDateTime internetLastLoginAt;

    @Size(max = 255)
    @Column(name = "mobile_pin_hash")
    private String mobilePinHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "mobile_pin_algo", length = 50)
    private String mobilePinAlgo;

    @Column(name = "mobile_pin_set_at")
    private OffsetDateTime mobilePinSetAt;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "mobile_first_time_login", nullable = false)
    private Boolean mobileFirstTimeLogin = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "mobile_failed_attempts", nullable = false)
    private Short mobileFailedAttempts;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "mobile_locked", nullable = false)
    private Boolean mobileLocked = false;

    @Column(name = "mobile_last_login_at")
    private OffsetDateTime mobileLastLoginAt;

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