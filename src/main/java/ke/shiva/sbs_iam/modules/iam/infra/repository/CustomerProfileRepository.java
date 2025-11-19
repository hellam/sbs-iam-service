package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfileEntity, Long> {

    Optional<CustomerProfileEntity> findByUserId(Long userId);
}
