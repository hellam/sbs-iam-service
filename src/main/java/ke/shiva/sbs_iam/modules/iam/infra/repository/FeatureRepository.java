package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<FeatureEntity, Long> {
    Optional<FeatureEntity> findByCode(String code);

    List<FeatureEntity> findByEnabledTrueAndChannelOrderByCategoryAscNameAsc(Channel channel);
}
