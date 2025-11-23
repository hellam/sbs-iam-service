package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyScope;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "policies", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class PolicyEntity extends BaseEntity {
    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Size(max = 50)
    @NotNull
    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'GLOBAL'")
    @Column(name = "scope", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PolicyScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private PartyEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id")
    private IamUserEntity iamUser;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @OneToOne(mappedBy = "policy")
    private MfaPolicyEntity mfaPolicy;

    @OneToOne(mappedBy = "policy")
    private PasswordPolicyEntity passwordPolicy;

    @OneToOne(mappedBy = "policy")
    private PinPolicyEntity pinPolicy;

    @OneToOne(mappedBy = "policy")
    private SecurityQuestionPolicyEntity securityQuestionPolicy;

}