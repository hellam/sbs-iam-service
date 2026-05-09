package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.LoginHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {

    List<LoginHistoryEntity> findByIamUserOrderByCreatedAtDesc(IamUserEntity iamUser);

    List<LoginHistoryEntity> findTop100ByIamUser_IdOrderByCreatedAtDesc(Long iamUserId);

    List<LoginHistoryEntity> findTop100ByIamUser_IdInOrderByCreatedAtDesc(Collection<Long> iamUserIds);

    List<LoginHistoryEntity> findByIamUserAndSuccessTrueOrderByCreatedAtDesc(IamUserEntity iamUser);

    Optional<LoginHistoryEntity> findFirstByIamUserAndSuccessTrueOrderByCreatedAtDesc(IamUserEntity iamUser);

    List<LoginHistoryEntity> findByIamUserAndCreatedAtAfterOrderByCreatedAtDesc(
            IamUserEntity iamUser, OffsetDateTime after);

    long deleteByCreatedAtBefore(OffsetDateTime cutoff);
}
