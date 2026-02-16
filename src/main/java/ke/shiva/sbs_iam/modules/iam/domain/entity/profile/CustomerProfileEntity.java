package ke.shiva.sbs_iam.modules.iam.domain.entity.profile;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "customer_profile", schema = "iam_service")
public class CustomerProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "iam_user_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @Size(max = 50)
    @Column(name = "core_customer_id", length = 50)
    private String coreCustomerId;

    @Size(max = 50)
    @Column(name = "segment", length = 50)
    private String segment;

    @Size(max = 10)
    @ColumnDefault("'en'")
    @Column(name = "language", length = 10)
    private String language;

    @Size(max = 50)
    @ColumnDefault("'Africa/Nairobi'")
    @Column(name = "timezone", length = 50)
    private String timezone;

    @Size(max = 20)
    @ColumnDefault("'light'")
    @Column(name = "theme", length = 20)
    private String theme;

    @ColumnDefault("true")
    @Column(name = "allow_email")
    private Boolean allowEmail;

    @ColumnDefault("true")
    @Column(name = "allow_sms")
    private Boolean allowSms;

    @ColumnDefault("false")
    @Column(name = "allow_push")
    private Boolean allowPush;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getFullName() {
        if (iamUser != null && iamUser.getParty() != null) {
            return iamUser.getParty().getPerson().getFullName();
        }
        return "";
    }
}