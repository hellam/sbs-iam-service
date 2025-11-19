package ke.shiva.sbs_iam.modules.iam.domain.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pin_history", schema = "iam_service")
public class PinHistoryEntity {
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
    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "pin_algo", length = 50)
    private String pinAlgo;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

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

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getPinAlgo() {
        return pinAlgo;
    }

    public void setPinAlgo(String pinAlgo) {
        this.pinAlgo = pinAlgo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

}