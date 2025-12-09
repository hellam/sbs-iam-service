package ke.shiva.sbs_iam.modules.iam.domain.entity.auth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "organization_user_auth", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class OrganizationUserAuthEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_user_id", nullable = false)
    private OrganizationUserEntity organizationUser;

    @Size(max = 255)
    @Column(name = "approver_password_hash")
    private String approverPasswordHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "approver_password_algo", length = 50)
    private String approverPasswordAlgo;

    @Size(max = 255)
    @Column(name = "approver_pin_hash")
    private String approverPinHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "approver_pin_algo", length = 50)
    private String approverPinAlgo;

    @Size(max = 255)
    @Column(name = "second_factor_secret")
    private String secondFactorSecret;

    @Column(name = "second_factor_last_verified_at")
    private OffsetDateTime secondFactorLastVerifiedAt;

    @ColumnDefault("0")
    @Column(name = "auth_approval_limit", precision = 18, scale = 2)
    private BigDecimal authApprovalLimit;

}