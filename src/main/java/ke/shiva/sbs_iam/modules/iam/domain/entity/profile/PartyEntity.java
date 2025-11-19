package ke.shiva.sbs_iam.modules.iam.domain.entity.profile;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.FeaturePolicyAssignmentEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "party", schema = "iam_service")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at"))
})
public class PartyEntity extends BaseEntity {
    @NotNull
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Size(max = 50)
    @NotNull
    @Column(name = "party_type", nullable = false, length = 50)
    private String partyType;

    @Size(max = 50)
    @Column(name = "core_customer_id", length = 50)
    private String coreCustomerId;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @OneToMany(mappedBy = "organization")
    private Set<FeaturePolicyAssignmentEntity> featurePolicyAssignments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "party")
    private Set<IamUserEntity> iamUsers = new LinkedHashSet<>();

    @OneToOne(mappedBy = "organization")
    private OrgRoleEntity orgRole;

    @OneToOne(mappedBy = "party")
    private OrganizationEntity organization;

    @OneToMany(mappedBy = "organizationParty")
    private Set<OrganizationUserEntity> organizationUsers = new LinkedHashSet<>();

    @OneToOne(mappedBy = "party")
    private PersonEntity person;

    @OneToMany(mappedBy = "organization")
    private Set<PolicyEntity> policies = new LinkedHashSet<>();

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(String partyType) {
        this.partyType = partyType;
    }

    public String getCoreCustomerId() {
        return coreCustomerId;
    }

    public void setCoreCustomerId(String coreCustomerId) {
        this.coreCustomerId = coreCustomerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<FeaturePolicyAssignmentEntity> getFeaturePolicyAssignments() {
        return featurePolicyAssignments;
    }

    public void setFeaturePolicyAssignments(Set<FeaturePolicyAssignmentEntity> featurePolicyAssignments) {
        this.featurePolicyAssignments = featurePolicyAssignments;
    }

    public Set<IamUserEntity> getIamUsers() {
        return iamUsers;
    }

    public void setIamUsers(Set<IamUserEntity> iamUsers) {
        this.iamUsers = iamUsers;
    }

    public OrgRoleEntity getOrgRole() {
        return orgRole;
    }

    public void setOrgRole(OrgRoleEntity orgRole) {
        this.orgRole = orgRole;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public Set<OrganizationUserEntity> getOrganizationUsers() {
        return organizationUsers;
    }

    public void setOrganizationUsers(Set<OrganizationUserEntity> organizationUsers) {
        this.organizationUsers = organizationUsers;
    }

    public PersonEntity getPerson() {
        return person;
    }

    public void setPerson(PersonEntity person) {
        this.person = person;
    }

    public Set<PolicyEntity> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<PolicyEntity> policies) {
        this.policies = policies;
    }

}