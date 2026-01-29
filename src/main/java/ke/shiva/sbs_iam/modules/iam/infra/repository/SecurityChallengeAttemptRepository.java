package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityChallengeAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SecurityChallengeAttemptRepository extends JpaRepository<SecurityChallengeAttemptEntity, Long> {

    /**
     * Find all attempts by user
     */
    List<SecurityChallengeAttemptEntity> findByIamUser(IamUserEntity iamUser);

    /**
     * Find recent attempts by user within a time window
     */
    List<SecurityChallengeAttemptEntity> findByIamUserAndCreatedAtAfter(
            IamUserEntity iamUser,
            OffsetDateTime after
    );

    /**
     * Count failed attempts by user within a time window
     */
    @Query("SELECT COUNT(s) FROM SecurityChallengeAttemptEntity s " +
           "WHERE s.iamUser = :user " +
           "AND s.answerCorrect = false " +
           "AND s.createdAt >= :after")
    long countFailedAttemptsByUserAfter(
            @Param("user") IamUserEntity user,
            @Param("after") OffsetDateTime after
    );

    /**
     * Count attempts by user and IP within a time window
     */
    @Query("SELECT COUNT(s) FROM SecurityChallengeAttemptEntity s " +
           "WHERE s.iamUser = :user " +
           "AND s.ipAddress = :ipAddress " +
           "AND s.createdAt >= :after")
    long countAttemptsByUserAndIpAfter(
            @Param("user") IamUserEntity user,
            @Param("ipAddress") String ipAddress,
            @Param("after") OffsetDateTime after
    );

    /**
     * Find attempts by device ID
     */
    List<SecurityChallengeAttemptEntity> findByDeviceIdAndCreatedAtAfter(
            String deviceId,
            OffsetDateTime after
    );
}
