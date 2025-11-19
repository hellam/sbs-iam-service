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
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.PasswordHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.FeaturePolicyAssignmentEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

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
    @ColumnDefault("'CUSTOMER'")
    @Column(name = "user_category", nullable = false, length = 50)
    private String userCategory;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'LOCAL'")
    @Column(name = "auth_provider", nullable = false, length = 50)
    private String authProvider;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

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
    private Set<FeaturePolicyAssignmentEntity> featurePolicyAssignments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<IamAuditLogEntity> iamAuditLogs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<IamUserSecurityQuestionEntity> iamUserSecurityQuestions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<LoginHistoryEntity> loginHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<LoginIdentifierEntity> loginIdentifiers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<OrganizationUserEntity> organizationUsers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<PasswordHistoryEntity> passwordHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<PinHistoryEntity> pinHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<PolicyEntity> policies = new LinkedHashSet<>();

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public PartyEntity getParty() {
        return party;
    }

    public void setParty(PartyEntity party) {
        this.party = party;
    }

    public String getUserCategory() {
        return userCategory;
    }

    public void setUserCategory(String userCategory) {
        this.userCategory = userCategory;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CustomerAuthEntity getCustomerAuth() {
        return customerAuth;
    }

    public void setCustomerAuth(CustomerAuthEntity customerAuth) {
        this.customerAuth = customerAuth;
    }

    public CustomerProfileEntity getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfileEntity customerProfile) {
        this.customerProfile = customerProfile;
    }

    public EmployeeAuthEntity getEmployeeAuth() {
        return employeeAuth;
    }

    public void setEmployeeAuth(EmployeeAuthEntity employeeAuth) {
        this.employeeAuth = employeeAuth;
    }

    public Set<EmployeePasswordHistoryEntity> getEmployeePasswordHistories() {
        return employeePasswordHistories;
    }

    public void setEmployeePasswordHistories(Set<EmployeePasswordHistoryEntity> employeePasswordHistories) {
        this.employeePasswordHistories = employeePasswordHistories;
    }

    public EmployeeProfileEntity getEmployeeProfile() {
        return employeeProfile;
    }

    public void setEmployeeProfile(EmployeeProfileEntity employeeProfile) {
        this.employeeProfile = employeeProfile;
    }

    public Set<FeaturePolicyAssignmentEntity> getFeaturePolicyAssignments() {
        return featurePolicyAssignments;
    }

    public void setFeaturePolicyAssignments(Set<FeaturePolicyAssignmentEntity> featurePolicyAssignments) {
        this.featurePolicyAssignments = featurePolicyAssignments;
    }

    public Set<IamAuditLogEntity> getIamAuditLogs() {
        return iamAuditLogs;
    }

    public void setIamAuditLogs(Set<IamAuditLogEntity> iamAuditLogs) {
        this.iamAuditLogs = iamAuditLogs;
    }

    public Set<IamUserSecurityQuestionEntity> getIamUserSecurityQuestions() {
        return iamUserSecurityQuestions;
    }

    public void setIamUserSecurityQuestions(Set<IamUserSecurityQuestionEntity> iamUserSecurityQuestions) {
        this.iamUserSecurityQuestions = iamUserSecurityQuestions;
    }

    public Set<LoginHistoryEntity> getLoginHistories() {
        return loginHistories;
    }

    public void setLoginHistories(Set<LoginHistoryEntity> loginHistories) {
        this.loginHistories = loginHistories;
    }

    public Set<LoginIdentifierEntity> getLoginIdentifiers() {
        return loginIdentifiers;
    }

    public void setLoginIdentifiers(Set<LoginIdentifierEntity> loginIdentifiers) {
        this.loginIdentifiers = loginIdentifiers;
    }

    public Set<OrganizationUserEntity> getOrganizationUsers() {
        return organizationUsers;
    }

    public void setOrganizationUsers(Set<OrganizationUserEntity> organizationUsers) {
        this.organizationUsers = organizationUsers;
    }

    public Set<PasswordHistoryEntity> getPasswordHistories() {
        return passwordHistories;
    }

    public void setPasswordHistories(Set<PasswordHistoryEntity> passwordHistories) {
        this.passwordHistories = passwordHistories;
    }

    public Set<PinHistoryEntity> getPinHistories() {
        return pinHistories;
    }

    public void setPinHistories(Set<PinHistoryEntity> pinHistories) {
        this.pinHistories = pinHistories;
    }

    public Set<PolicyEntity> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<PolicyEntity> policies) {
        this.policies = policies;
    }

    public Set<SecurityChallengeAttemptEntity> getSecurityChallengeAttempts() {
        return securityChallengeAttempts;
    }

    public void setSecurityChallengeAttempts(Set<SecurityChallengeAttemptEntity> securityChallengeAttempts) {
        this.securityChallengeAttempts = securityChallengeAttempts;
    }

    public Set<SecurityEventEntity> getSecurityEvents() {
        return securityEvents;
    }

    public void setSecurityEvents(Set<SecurityEventEntity> securityEvents) {
        this.securityEvents = securityEvents;
    }

    public Set<SessionEntity> getSessions() {
        return sessions;
    }

    public void setSessions(Set<SessionEntity> sessions) {
        this.sessions = sessions;
    }

    public Set<TrustedDeviceEntity> getTrustedDevices() {
        return trustedDevices;
    }

    public void setTrustedDevices(Set<TrustedDeviceEntity> trustedDevices) {
        this.trustedDevices = trustedDevices;
    }

    public Set<UserConsentEntity> getUserConsents() {
        return userConsents;
    }

    public void setUserConsents(Set<UserConsentEntity> userConsents) {
        this.userConsents = userConsents;
    }

}