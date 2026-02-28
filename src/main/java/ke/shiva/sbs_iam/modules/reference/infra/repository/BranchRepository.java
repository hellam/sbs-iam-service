package ke.shiva.sbs_iam.modules.reference.infra.repository;

import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {
    Optional<BranchEntity> findByBranchCode(String branchCode);

    Optional<BranchEntity> findFirstByOrderByIdAsc();
}
