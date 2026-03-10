package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.client.iam.enums.TaskRole;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUserEntity, Long> {

    @EntityGraph(attributePaths = {"organizationParty", "organizationParty.organization"})
    List<OrganizationUserEntity> findAllByIamUser(IamUserEntity iamUser);

    Optional<OrganizationUserEntity> findByIamUserAndOrganizationParty(IamUserEntity iamUser, PartyEntity organizationParty);

    @Query("""
            SELECT DISTINCT ou.iamUser.id
              FROM OrganizationUserEntity ou
             WHERE ou.organizationParty.coreCustomerId = :customerId
               AND ou.orgRole.taskRole IN :taskRoles
               AND UPPER(ou.status) = 'ACTIVE'
               AND COALESCE(ou.orgRole.isActive, false) = true
               AND COALESCE(ou.organizationParty.organization.accountLocked, false) = false
               AND ou.iamUser.status = :iamStatus
            """)
    List<Long> findActiveIamUserIdsByCustomerAndTaskRoles(@Param("customerId") String customerId,
                                                          @Param("taskRoles") Collection<TaskRole> taskRoles,
                                                          @Param("iamStatus") IamStatus iamStatus);

    @EntityGraph(attributePaths = {
            "iamUser",
            "iamUser.party",
            "iamUser.party.person",
            "iamUser.customerProfile",
            "iamUser.customerAuth",
            "iamUser.contacts",
            "orgRole",
            "organizationParty",
            "organizationParty.organization"
    })
    List<OrganizationUserEntity> findByOrganizationParty_CoreCustomerIdOrderByCreatedAtDesc(String coreCustomerId);
}
