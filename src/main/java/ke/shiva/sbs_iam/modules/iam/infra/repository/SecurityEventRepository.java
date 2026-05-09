package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, Long> {
    List<SecurityEventEntity> findTop100ByIamUser_IdOrderByCreatedAtDesc(Long iamUserId);

    List<SecurityEventEntity> findTop100ByIamUser_IdInOrderByCreatedAtDesc(Collection<Long> iamUserIds);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
