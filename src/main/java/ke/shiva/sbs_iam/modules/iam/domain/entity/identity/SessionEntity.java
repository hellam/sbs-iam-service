package ke.shiva.sbs_iam.modules.iam.domain.entity.identity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.OtpRecordEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "sessions", schema = "iam_service")
public class SessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @Size(max = 100)
    @NotNull
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "channel", length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Size(max = 255)
    @Column(name = "device_id")
    private String deviceId;

    @Size(max = 50)
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = Integer.MAX_VALUE)
    private String userAgent;

    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LoginStage status;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "last_access_at")
    private OffsetDateTime lastAccessAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", length = Integer.MAX_VALUE)
    private String revokedReason;

    @NotNull
    @Column(name = "session_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SessionType sessionType;

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "session")
    private Set<RefreshTokenEntity> refreshTokens = new LinkedHashSet<>();

    @OneToMany(mappedBy = "relatedSession")
    private Set<SecurityEventEntity> securityEvents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "session")
    private Set<SessionEventEntity> sessionEvents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "sessionId")
    private Set<OtpRecordEntity> otpRecordEntities = new LinkedHashSet<>();

    @Column(name = "profile_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ProfileType profileType;

    @Column(name = "profile_id")
    private Long profileId;

}