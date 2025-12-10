package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyType;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;

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

    @NotNull
    @Column(name = "policy_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PolicyType policyType;

    @NotNull
    @Type(JsonType.class)
    @Column(name = "channels", columnDefinition = "jsonb")
    private Channel[] channels;

    @Size(max = 255)
    @Column(name = "name")
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @OneToOne(mappedBy = "policy")
    private MfaPolicyEntity mfaPolicy;

    @OneToOne(mappedBy = "policy")
    private PasswordPolicyEntity passwordPolicy;

    @OneToOne(mappedBy = "policy")
    private PinPolicyEntity pinPolicy;

    @OneToOne(mappedBy = "policy")
    private SecurityQuestionPolicyEntity securityQuestionPolicy;

}