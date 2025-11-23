package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.PasswordHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, Long> {

   @Query("""
           SELECT ph.passwordHash
           FROM PasswordHistoryEntity ph
           WHERE ph.iamUser = :iamUser
           ORDER BY ph.createdAt DESC
           LIMIT :limit
           """)
   List<String> findPasswordHashesByIamUser(IamUserEntity iamUser, int limit);
}
