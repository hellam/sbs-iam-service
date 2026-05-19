package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginIdentifierRepository extends JpaRepository<LoginIdentifierEntity, Long> {
    @EntityGraph(attributePaths = {"iamUser"})
    Optional<LoginIdentifierEntity> findByIdentifierAndChannelAndStatus(String identifier, Channel channel, IamStatus status);

    Optional<LoginIdentifierEntity> findByIdentifierAndIdentifierType(String identifier, String identifierType);

    boolean existsByChannelAndIdentifierTypeAndIdentifier(
            Channel channel,
            String identifierType,
            String identifier
    );

    Optional<LoginIdentifierEntity> findByIamUserAndChannelAndIdentifierType(
            IamUserEntity iamUser,
            Channel channel,
            String identifierType
    );

    Optional<LoginIdentifierEntity> findFirstByIamUserAndChannelOrderByIdAsc(
            IamUserEntity iamUser,
            Channel channel
    );

    //find by identifier and channel, regardless of status
    Optional<LoginIdentifierEntity> findByIdentifierAndChannel(String identifier, Channel channel);

}
