package ke.shiva.sbs_iam.modules.iam.domain.entity.security;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "iam_user_security_question", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class IamUserSecurityQuestionEntity extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_question_id", nullable = false)
    private SecurityQuestionEntity securityQuestion;

    @Size(max = 255)
    @NotNull
    @Column(name = "answer_hash", nullable = false)
    private String answerHash;

    @Size(max = 50)
    @ColumnDefault("'bcrypt'")
    @Column(name = "answer_algo", length = 50)
    private String answerAlgo;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    public IamUserEntity getIamUser() {
        return iamUser;
    }

    public void setIamUser(IamUserEntity iamUser) {
        this.iamUser = iamUser;
    }

    public SecurityQuestionEntity getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(SecurityQuestionEntity securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getAnswerHash() {
        return answerHash;
    }

    public void setAnswerHash(String answerHash) {
        this.answerHash = answerHash;
    }

    public String getAnswerAlgo() {
        return answerAlgo;
    }

    public void setAnswerAlgo(String answerAlgo) {
        this.answerAlgo = answerAlgo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

}