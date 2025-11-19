package ke.shiva.sbs_iam.modules.iam.domain.entity.audit;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;


@Entity
@Table(name = "employee_password_history", schema = "iam_service")
@Getter
@Setter
public class EmployeePasswordHistoryEntity {
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
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "password_algo", length = 50)
    private String passwordAlgo;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}