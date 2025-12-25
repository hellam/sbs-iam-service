package ke.shiva.sbs_iam.modules.iam.domain.entity.profile;

import jakarta.persistence.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginProfiles;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Entity
@Table(name = "profile_contact", schema = "iam_service")
public class ProfileContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "iam_user_id", nullable = false)
    private IamUserEntity iamUser;

    @ManyToOne
    @JoinColumn(name = "user_contact_id", nullable = false)
    private UserContact userContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false)
    private LoginProfiles profileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false)
    private ContactType contactType;
}

