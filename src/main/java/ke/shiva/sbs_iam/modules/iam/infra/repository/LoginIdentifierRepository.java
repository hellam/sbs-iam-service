package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginIdentifierRepository extends JpaRepository<LoginIdentifierEntity, Long> {
    Optional<LoginIdentifierEntity> findByIdentifierAndChannelAndStatus(String identifier, Channel channel, IamStatus status);

    Optional<LoginIdentifierEntity> findByIdentifierAndIdentifierType(String identifier, String identifierType);
}
