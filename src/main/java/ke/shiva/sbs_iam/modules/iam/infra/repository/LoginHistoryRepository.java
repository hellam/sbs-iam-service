package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.LoginHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistoryEntity, Long> {

    List<LoginHistoryEntity> findByIamUserOrderByCreatedAtDesc(IamUserEntity iamUser);

    List<LoginHistoryEntity> findByIamUserAndSuccessTrueOrderByCreatedAtDesc(IamUserEntity iamUser);

    List<LoginHistoryEntity> findByIamUserAndCreatedAtAfterOrderByCreatedAtDesc(
            IamUserEntity iamUser, OffsetDateTime after);
}

