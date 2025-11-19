package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {}
