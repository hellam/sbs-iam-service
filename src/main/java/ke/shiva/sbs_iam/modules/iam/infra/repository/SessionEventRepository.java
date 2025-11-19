package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionEventRepository extends JpaRepository<SessionEventEntity, Long> {}

