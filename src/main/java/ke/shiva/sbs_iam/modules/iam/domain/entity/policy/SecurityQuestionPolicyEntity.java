package ke.shiva.sbs_iam.modules.iam.domain.entity.policy;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Setter
@Getter
@Entity
@Table(name = "security_question_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class SecurityQuestionPolicyEntity extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private PolicyEntity policy;

    @NotNull
    @Column(name = "channel", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @ColumnDefault("false")
    @Column(name = "enabled")
    private Boolean enabled = false;

    @ColumnDefault("0")
    @Column(name = "min_questions")
    private Short minQuestions = (short) 0;

    @ColumnDefault("0")
    @Column(name = "max_questions")
    private Short maxQuestions = (short) 0;

    @ColumnDefault("false")
    @Column(name = "mandatory")
    private Boolean mandatory = false;

    @ColumnDefault("false")
    @Column(name = "ask_on_forgot_password")
    private Boolean askOnForgotPassword = false;

    @ColumnDefault("false")
    @Column(name = "ask_on_sensitive_action")
    private Boolean askOnSensitiveAction = false;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive = true;

    @NotNull
    @ColumnDefault("3")
    @Column(name = "max_verify_attempts", nullable = false)
    private Short maxVerifyAttempts = (short) 3;

}
