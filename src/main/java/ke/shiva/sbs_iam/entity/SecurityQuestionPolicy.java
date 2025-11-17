package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "security_question_policy", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class SecurityQuestionPolicy extends BaseEntity {
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Size(max = 50)
    @NotNull
    @Column(name = "channel", nullable = false, length = 50)
    private String channel;

    @ColumnDefault("false")
    @Column(name = "enabled")
    private Boolean enabled;

    @ColumnDefault("0")
    @Column(name = "min_questions")
    private Short minQuestions;

    @ColumnDefault("0")
    @Column(name = "max_questions")
    private Short maxQuestions;

    @ColumnDefault("false")
    @Column(name = "mandatory")
    private Boolean mandatory;

    @ColumnDefault("false")
    @Column(name = "ask_on_forgot_password")
    private Boolean askOnForgotPassword;

    @ColumnDefault("false")
    @Column(name = "ask_on_sensitive_action")
    private Boolean askOnSensitiveAction;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Short getMinQuestions() {
        return minQuestions;
    }

    public void setMinQuestions(Short minQuestions) {
        this.minQuestions = minQuestions;
    }

    public Short getMaxQuestions() {
        return maxQuestions;
    }

    public void setMaxQuestions(Short maxQuestions) {
        this.maxQuestions = maxQuestions;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Boolean getAskOnForgotPassword() {
        return askOnForgotPassword;
    }

    public void setAskOnForgotPassword(Boolean askOnForgotPassword) {
        this.askOnForgotPassword = askOnForgotPassword;
    }

    public Boolean getAskOnSensitiveAction() {
        return askOnSensitiveAction;
    }

    public void setAskOnSensitiveAction(Boolean askOnSensitiveAction) {
        this.askOnSensitiveAction = askOnSensitiveAction;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

}