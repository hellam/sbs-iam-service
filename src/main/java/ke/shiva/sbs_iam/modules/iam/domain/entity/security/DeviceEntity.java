package ke.shiva.sbs_iam.modules.iam.domain.entity.security;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "devices", schema = "iam_service")
public class DeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 128)
    @NotNull
    @Column(name = "device_id", nullable = false, unique = true, length = 128)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id")
    private IamUserEntity iamUser;

    @Size(max = 32)
    @NotNull
    @Column(name = "device_type", nullable = false, length = 32)
    private String deviceType;

    @Size(max = 64)
    @Column(name = "platform", length = 64)
    private String platform;

    @Size(max = 64)
    @Column(name = "browser", length = 64)
    private String browser;

    @Size(max = 32)
    @Column(name = "browser_version", length = 32)
    private String browserVersion;

    @Size(max = 64)
    @NotNull
    @Column(name = "user_agent_hash", nullable = false, length = 64)
    private String userAgentHash;

    @Size(max = 45)
    @NotNull
    @Column(name = "first_ip", nullable = false, length = 45)
    private String firstIp;

    @Size(max = 45)
    @NotNull
    @Column(name = "last_ip", nullable = false, length = 45)
    private String lastIp;

    @NotNull
    @Column(name = "first_seen_at", nullable = false)
    private OffsetDateTime firstSeenAt;

    @NotNull
    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Size(max = 2)
    @Column(name = "first_country", length = 2)
    private String firstCountry;

    @Size(max = 2)
    @Column(name = "last_country", length = 2)
    private String lastCountry;

    @Size(max = 32)
    @Column(name = "last_asn", length = 32)
    private String lastAsn;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "trusted", nullable = false)
    private Boolean trusted = false;

    @Size(max = 16)
    @NotNull
    @ColumnDefault("'LOW'")
    @Column(name = "risk_level", nullable = false, length = 16)
    private String riskLevel;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Size(max = 16)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Size(max = 255)
    @Column(name = "revoke_reason")
    private String revokeReason;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}