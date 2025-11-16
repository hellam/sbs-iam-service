package ke.shiva.sbs_iam.modules.payment.repository;

import ke.shiva.sbs_iam.modules.payment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByEmail(String email);
    User findByUsername(String username);
}
