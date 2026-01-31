package ke.shiva.sbs_iam.modules.iam.domain.entity.profile;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.FeaturePolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.rbac.OrgRoleEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.shivacorestarter.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
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

    @NotNull
    @Column(name = "party_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PartyType partyType;

    @Size(max = 50)
    @Column(name = "core_customer_id", length = 50)
    private String coreCustomerId;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @OneToMany(mappedBy = "organization")
    private Set<FeaturePolicyEntity> featurePolicy = new LinkedHashSet<>();

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

}