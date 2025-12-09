package ke.shiva.sbs_iam.modules.iam.domain.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Setter
@Getter
@Entity
@Table(name = "iam_audit_log", schema = "iam_service")
public class IamAuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id")
    private IamUserEntity iamUser;

    @Size(max = 50)
    @Column(name = "user_category", length = 50)
    private String userCategory;

    @Size(max = 100)
    @NotNull
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Size(max = 50)
    @Column(name = "channel", length = 50)
    private String channel;

    @Size(max = 50)
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Size(max = 255)
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "user_agent", length = Integer.MAX_VALUE)
    private String userAgent;

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "entity_id")
    private Long entityId;

    @Size(max = 100)
    @Column(name = "entity_type", length = 100)
    private String entityType;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

}