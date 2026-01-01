package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.security.OtpRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface OtpRecordRepository extends JpaRepository<OtpRecordEntity, Long> {
    Optional<OtpRecordEntity> findBySessionId(String sessionId);
    long countByToAndCreatedAtAfter(String to, OffsetDateTime after);
}
