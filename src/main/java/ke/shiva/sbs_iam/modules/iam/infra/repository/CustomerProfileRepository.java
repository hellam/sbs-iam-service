package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.party.PartyType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, Long>,
        JpaSpecificationExecutor<CustomerProfileEntity> {

    Optional<CustomerProfileEntity> findByIamUser(IamUserEntity iamUser);
    Optional<CustomerProfileEntity> findByIamUserAndIsVerifiedTrue(IamUserEntity iamUser);

    Optional<CustomerProfileEntity> findByCoreCustomerId(String coreCustomerId);
    Optional<CustomerProfileEntity> findByCoreCustomerIdIgnoreCase(String coreCustomerId);

    @Query("""
            select distinct profile
            from CustomerProfileEntity profile
            join profile.iamUser iamUser
            join iamUser.party party
            left join party.person person
            left join iamUser.contacts contact
            left join iamUser.loginIdentifiers identifier
            where party.partyType = :partyType
              and (:status is null or iamUser.status = :status)
              and (:isVerified is null or profile.isVerified = :isVerified)
              and (
                lower(coalesce(profile.coreCustomerId, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(person.fullName, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(contact.contactValue, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(identifier.identifier, '')) like lower(concat('%', :search, '%'))
              )
            """)
    Page<CustomerProfileEntity> searchBackofficeCustomers(
            @Param("search") String search,
            @Param("partyType") PartyType partyType,
            @Param("status") IamStatus status,
            @Param("isVerified") Boolean isVerified,
            Pageable pageable
    );
}
