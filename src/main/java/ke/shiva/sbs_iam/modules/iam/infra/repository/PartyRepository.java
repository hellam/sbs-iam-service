package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.PartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, Long> {
    boolean existsByCoreCustomerId(String coreCustomerId);
}

