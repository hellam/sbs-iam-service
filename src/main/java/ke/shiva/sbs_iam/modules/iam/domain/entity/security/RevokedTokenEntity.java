package ke.shiva.sbs_iam.modules.iam.domain.entity.security;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "revoked_tokens", schema = "iam_service")
public class RevokedTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true)
    private String jti;

    @Column(name = "expiry_date", nullable = false)
    private OffsetDateTime expiryDate;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;
}

