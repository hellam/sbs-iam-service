package ke.shiva.sbs_iam.modules.iam.domain.entity.identity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "refresh_tokens", schema = "iam_service")
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionEntity session;

    @Size(max = 255)
    @NotNull
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @ColumnDefault("now()")
    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", length = Integer.MAX_VALUE)
    private String revokedReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_token_id")
    private RefreshTokenEntity replacedByToken;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "replacedByToken")
    private Set<RefreshTokenEntity> refreshTokens = new LinkedHashSet<>();

}