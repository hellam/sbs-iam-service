package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "security_question", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class SecurityQuestion extends BaseEntity {
    @Size(max = 255)
    @NotNull
    @Column(name = "question", nullable = false)
    private String question;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "securityQuestion")
    private Set<IamUserSecurityQuestion> iamUserSecurityQuestions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "securityQuestion")
    private Set<SecurityChallengeAttempt> securityChallengeAttempts = new LinkedHashSet<>();

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Set<IamUserSecurityQuestion> getIamUserSecurityQuestions() {
        return iamUserSecurityQuestions;
    }

    public void setIamUserSecurityQuestions(Set<IamUserSecurityQuestion> iamUserSecurityQuestions) {
        this.iamUserSecurityQuestions = iamUserSecurityQuestions;
    }

    public Set<SecurityChallengeAttempt> getSecurityChallengeAttempts() {
        return securityChallengeAttempts;
    }

    public void setSecurityChallengeAttempts(Set<SecurityChallengeAttempt> securityChallengeAttempts) {
        this.securityChallengeAttempts = securityChallengeAttempts;
    }

}