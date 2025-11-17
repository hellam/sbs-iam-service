package ke.shiva.sbs_iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "iam_user", schema = "iam_service")
public class IamUser {
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
    private Party party;

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
    private CustomerAuth customerAuth;

    @OneToOne(mappedBy = "iamUser")
    private CustomerProfile customerProfile;

    @OneToOne(mappedBy = "iamUser")
    private EmployeeAuth employeeAuth;

    @OneToMany(mappedBy = "iamUser")
    private Set<EmployeePasswordHistory> employeePasswordHistories = new LinkedHashSet<>();

    @OneToOne(mappedBy = "iamUser")
    private EmployeeProfile employeeProfile;

    @OneToMany(mappedBy = "iamUser")
    private Set<FeaturePolicyAssignment> featurePolicyAssignments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<IamAuditLog> iamAuditLogs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<IamUserSecurityQuestion> iamUserSecurityQuestions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<LoginHistory> loginHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<LoginIdentifier> loginIdentifiers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<OrganizationUser> organizationUsers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<PasswordHistory> passwordHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<PinHistory> pinHistories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<Policy> policies = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<SecurityChallengeAttempt> securityChallengeAttempts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<SecurityEvent> securityEvents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<Session> sessions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<TrustedDevice> trustedDevices = new LinkedHashSet<>();

    @OneToMany(mappedBy = "iamUser")
    private Set<UserConsent> userConsents = new LinkedHashSet<>();

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

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
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

    public CustomerAuth getCustomerAuth() {
        return customerAuth;
    }

    public void setCustomerAuth(CustomerAuth customerAuth) {
        this.customerAuth = customerAuth;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public EmployeeAuth getEmployeeAuth() {
        return employeeAuth;
    }

    public void setEmployeeAuth(EmployeeAuth employeeAuth) {
        this.employeeAuth = employeeAuth;
    }

    public Set<EmployeePasswordHistory> getEmployeePasswordHistories() {
        return employeePasswordHistories;
    }

    public void setEmployeePasswordHistories(Set<EmployeePasswordHistory> employeePasswordHistories) {
        this.employeePasswordHistories = employeePasswordHistories;
    }

    public EmployeeProfile getEmployeeProfile() {
        return employeeProfile;
    }

    public void setEmployeeProfile(EmployeeProfile employeeProfile) {
        this.employeeProfile = employeeProfile;
    }

    public Set<FeaturePolicyAssignment> getFeaturePolicyAssignments() {
        return featurePolicyAssignments;
    }

    public void setFeaturePolicyAssignments(Set<FeaturePolicyAssignment> featurePolicyAssignments) {
        this.featurePolicyAssignments = featurePolicyAssignments;
    }

    public Set<IamAuditLog> getIamAuditLogs() {
        return iamAuditLogs;
    }

    public void setIamAuditLogs(Set<IamAuditLog> iamAuditLogs) {
        this.iamAuditLogs = iamAuditLogs;
    }

    public Set<IamUserSecurityQuestion> getIamUserSecurityQuestions() {
        return iamUserSecurityQuestions;
    }

    public void setIamUserSecurityQuestions(Set<IamUserSecurityQuestion> iamUserSecurityQuestions) {
        this.iamUserSecurityQuestions = iamUserSecurityQuestions;
    }

    public Set<LoginHistory> getLoginHistories() {
        return loginHistories;
    }

    public void setLoginHistories(Set<LoginHistory> loginHistories) {
        this.loginHistories = loginHistories;
    }

    public Set<LoginIdentifier> getLoginIdentifiers() {
        return loginIdentifiers;
    }

    public void setLoginIdentifiers(Set<LoginIdentifier> loginIdentifiers) {
        this.loginIdentifiers = loginIdentifiers;
    }

    public Set<OrganizationUser> getOrganizationUsers() {
        return organizationUsers;
    }

    public void setOrganizationUsers(Set<OrganizationUser> organizationUsers) {
        this.organizationUsers = organizationUsers;
    }

    public Set<PasswordHistory> getPasswordHistories() {
        return passwordHistories;
    }

    public void setPasswordHistories(Set<PasswordHistory> passwordHistories) {
        this.passwordHistories = passwordHistories;
    }

    public Set<PinHistory> getPinHistories() {
        return pinHistories;
    }

    public void setPinHistories(Set<PinHistory> pinHistories) {
        this.pinHistories = pinHistories;
    }

    public Set<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<Policy> policies) {
        this.policies = policies;
    }

    public Set<SecurityChallengeAttempt> getSecurityChallengeAttempts() {
        return securityChallengeAttempts;
    }

    public void setSecurityChallengeAttempts(Set<SecurityChallengeAttempt> securityChallengeAttempts) {
        this.securityChallengeAttempts = securityChallengeAttempts;
    }

    public Set<SecurityEvent> getSecurityEvents() {
        return securityEvents;
    }

    public void setSecurityEvents(Set<SecurityEvent> securityEvents) {
        this.securityEvents = securityEvents;
    }

    public Set<Session> getSessions() {
        return sessions;
    }

    public void setSessions(Set<Session> sessions) {
        this.sessions = sessions;
    }

    public Set<TrustedDevice> getTrustedDevices() {
        return trustedDevices;
    }

    public void setTrustedDevices(Set<TrustedDevice> trustedDevices) {
        this.trustedDevices = trustedDevices;
    }

    public Set<UserConsent> getUserConsents() {
        return userConsents;
    }

    public void setUserConsents(Set<UserConsent> userConsents) {
        this.userConsents = userConsents;
    }

}