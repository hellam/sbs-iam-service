package ke.shiva.sbs_iam.modules.iam.domain.entity.security;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "security_question", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class SecurityQuestionEntity extends BaseEntity {
    @Size(max = 255)
    @NotNull
    @Column(name = "question", nullable = false)
    private String question;

    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "securityQuestion")
    private Set<IamUserSecurityQuestionEntity> iamUserSecurityQuestions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "securityQuestion")
    private Set<SecurityChallengeAttemptEntity> securityChallengeAttempts = new LinkedHashSet<>();

}