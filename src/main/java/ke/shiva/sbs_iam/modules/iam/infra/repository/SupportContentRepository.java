package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.system.SupportContentEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.backoffice.SupportContentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportContentRepository extends JpaRepository<SupportContentEntity, Long> {

    List<SupportContentEntity> findByCategoryOrderBySortOrderAscUpdatedAtDesc(SupportContentCategory category);
}
