package ke.shiva.sbs_iam.modules.iam.infra.repository;

import io.lettuce.core.dynamic.annotation.Param;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {
    SessionEntity findBySessionId(String sessionId);
    @Query("SELECT s FROM SessionEntity s JOIN FETCH s.iamUser WHERE s.id = :id")
    Optional<SessionEntity> findByIdWithIamUser(@Param("id") Long id);
}
