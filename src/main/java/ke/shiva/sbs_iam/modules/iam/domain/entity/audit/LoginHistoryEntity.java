package ke.shiva.sbs_iam.modules.iam.domain.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "login_history", schema = "iam_service")
public class LoginHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id")
    private IamUserEntity iamUser;

    @Size(max = 50)
    @Column(name = "channel", length = 50)
    private String channel;

    @Size(max = 255)
    @Column(name = "identifier_used")
    private String identifierUsed;

    @ColumnDefault("false")
    @Column(name = "success")
    private Boolean success;

    @Size(max = 100)
    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Size(max = 50)
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Size(max = 255)
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "user_agent", length = Integer.MAX_VALUE)
    private String userAgent;

    @Size(max = 2)
    @Column(name = "location_country", length = 2)
    private String locationCountry;

    @Size(max = 100)
    @Column(name = "location_city", length = 100)
    private String locationCity;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

}