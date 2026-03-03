package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.EmployeeProfileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfileEntity, Long>,
        JpaSpecificationExecutor<EmployeeProfileEntity> {
    boolean existsByStaffNo(String staffNo);

    @EntityGraph(attributePaths = {"iamUser", "iamUser.party", "iamUser.party.person"})
    List<EmployeeProfileEntity> findByIamUser_Party_CoreCustomerIdOrderByCreatedAtDesc(String coreCustomerId);
}
