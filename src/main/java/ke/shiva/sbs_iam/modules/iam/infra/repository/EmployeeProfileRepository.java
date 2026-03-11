package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfileEntity, Long>,
        JpaSpecificationExecutor<EmployeeProfileEntity> {
    boolean existsByStaffNo(String staffNo);

    Optional<EmployeeProfileEntity> findFirstByIamUser_Party_CoreCustomerId(String coreCustomerId);
    Optional<EmployeeProfileEntity> findFirstByIamUser_Party_CoreCustomerIdIgnoreCase(String coreCustomerId);
}
