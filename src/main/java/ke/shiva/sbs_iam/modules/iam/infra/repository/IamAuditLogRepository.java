package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.IamAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IamAuditLogRepository extends JpaRepository<IamAuditLogEntity, Long> {

    List<IamAuditLogEntity> findTop100ByIamUser_IdOrderByCreatedAtDesc(Long iamUserId);

    List<IamAuditLogEntity> findTop100ByIamUser_IdInOrderByCreatedAtDesc(Collection<Long> iamUserIds);

    List<IamAuditLogEntity> findTop100ByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}
