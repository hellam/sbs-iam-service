package ke.shiva.sbs_iam.modules.iam.domain.entity.security;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "trusted_devices", schema = "iam_service")
public class TrustedDeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @Size(max = 255)
    @NotNull
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Size(max = 255)
    @Column(name = "device_name")
    private String deviceName;

    @Size(max = 50)
    @Column(name = "platform", length = 50)
    private String platform;

    @ColumnDefault("now()")
    @Column(name = "first_seen_at")
    private OffsetDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Size(max = 50)
    @Column(name = "last_ip_address", length = 50)
    private String lastIpAddress;

    @ColumnDefault("false")
    @Column(name = "is_trusted")
    private Boolean isTrusted;

    @Column(name = "mfa_verified_at")
    private OffsetDateTime mfaVerifiedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IamUserEntity getIamUser() {
        return iamUser;
    }

    public void setIamUser(IamUserEntity iamUser) {
        this.iamUser = iamUser;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public OffsetDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(OffsetDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public OffsetDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(OffsetDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getLastIpAddress() {
        return lastIpAddress;
    }

    public void setLastIpAddress(String lastIpAddress) {
        this.lastIpAddress = lastIpAddress;
    }

    public Boolean getIsTrusted() {
        return isTrusted;
    }

    public void setIsTrusted(Boolean isTrusted) {
        this.isTrusted = isTrusted;
    }

    public OffsetDateTime getMfaVerifiedAt() {
        return mfaVerifiedAt;
    }

    public void setMfaVerifiedAt(OffsetDateTime mfaVerifiedAt) {
        this.mfaVerifiedAt = mfaVerifiedAt;
    }

}