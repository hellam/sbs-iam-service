package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
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
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfileEntity, Long>,
        JpaSpecificationExecutor<EmployeeProfileEntity> {
    boolean existsByStaffNo(String staffNo);

    Optional<EmployeeProfileEntity> findFirstByIamUser_Party_CoreCustomerId(String coreCustomerId);
    Optional<EmployeeProfileEntity> findFirstByIamUser_Party_CoreCustomerIdIgnoreCase(String coreCustomerId);

    @Query("""
            select distinct profile
            from EmployeeProfileEntity profile
            join profile.iamUser iamUser
            join iamUser.party party
            left join party.person person
            left join iamUser.contacts contact
            left join iamUser.loginIdentifiers identifier
            where party.partyType = :partyType
              and (:status is null or iamUser.status = :status)
              and (
                lower(coalesce(party.coreCustomerId, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(person.fullName, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(profile.staffNo, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(profile.jobTitle, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(profile.department, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(contact.contactValue, '')) like lower(concat('%', :search, '%'))
                or lower(coalesce(identifier.identifier, '')) like lower(concat('%', :search, '%'))
              )
            """)
    Page<EmployeeProfileEntity> searchBackofficeEmployees(
            @Param("search") String search,
            @Param("partyType") PartyType partyType,
            @Param("status") IamStatus status,
            Pageable pageable
    );
}
