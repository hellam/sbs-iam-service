package ke.shiva.sbs_iam.modules.iam.infra.repository;

import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.ProfileContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileContactRepository extends JpaRepository<ProfileContact, Long> {
}

