package ke.shiva.sbs_iam.modules.iam.domain.entity.identity;

import jakarta.persistence.*;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "iam_user_contact", schema = "iam_service")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class UserContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 50)
    private ContactType contactType;

    @Column(name = "contact_value", nullable = false)
    private String contactValue;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}

