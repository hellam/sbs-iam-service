package ke.shiva.sbs_iam.modules.iam.domain.entity.identity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.EmployeePasswordHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.IamAuditLogEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.LoginHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.PinHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.PasswordHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.FeaturePolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.*;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "iam_user", schema = "iam_service")
public class IamUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private PartyEntity party;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'LOCAL'")
    @Column(name = "auth_provider", nullable = false, length = 50)
    private String authProvider;

    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private IamStatus status;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToOne(mappedBy = "iamUser")
    private CustomerAuthEntity customerAuth;

    @OneToOne(mappedBy = "iamUser")
    private CustomerProfileEntity customerProfile;

    @OneToOne(mappedBy = "iamUser")
    private EmployeeAuthEntity employeeAuth;

    @OneToMany(mappedBy = "iamUser")
    private Set<EmployeePasswordHistoryEntity> employeePasswordHistories = new LinkedHashSet<>();

    @OneToOne(mappedBy = "iamUser")
    private EmployeeProfileEntity employeeProfile;

    @OneToMany(mappedBy = "iamUser")
    private Set<FeaturePolicyEntity> featurePolicy = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<IamAuditLogEntity> iamAuditLogs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<IamUserSecurityQuestionEntity> iamUserSecurityQuestions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<LoginHistoryEntity> loginHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<LoginIdentifierEntity> loginIdentifiers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserContact> contacts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<OrganizationUserEntity> organizationUsers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<PasswordHistoryEntity> passwordHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<PinHistoryEntity> pinHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<SecurityChallengeAttemptEntity> securityChallengeAttempts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<SecurityEventEntity> securityEvents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<SessionEntity> sessions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<TrustedDeviceEntity> trustedDevices = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<UserConsentEntity> userConsents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<ProfileContact> profileContacts = new LinkedHashSet<>();

}

