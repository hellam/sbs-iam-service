package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginIdentifierRepository extends JpaRepository<LoginIdentifierEntity, Long> {
    LoginIdentifierEntity findByIdentifierAndChannelAndStatus(String identifier, Channel channel, IamStatus status);
}
