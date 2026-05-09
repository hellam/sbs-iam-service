package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SessionEventRepository extends JpaRepository<SessionEventEntity, Long> {
    List<SessionEventEntity> findTop100BySession_IamUser_IdOrderByEventAtDesc(Long iamUserId);

    List<SessionEventEntity> findTop100BySession_IamUser_IdInOrderByEventAtDesc(Collection<Long> iamUserIds);

    long deleteByEventAtBefore(OffsetDateTime cutoff);
}
