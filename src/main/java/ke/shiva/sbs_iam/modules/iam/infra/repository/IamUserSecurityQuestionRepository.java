package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.security.IamUserSecurityQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IamUserSecurityQuestionRepository extends JpaRepository<IamUserSecurityQuestionEntity, Long> {
    void deleteAllByIamUserId(Long id);
}