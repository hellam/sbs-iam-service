package ke.shiva.sbs_iam.modules.reference.infra.repository;

import java.util.Optional;

import io.lettuce.core.Value;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;

@Repository
public interface CountryRepository extends JpaRepository<CountryEntity, Long> {
    Optional<CountryEntity> findByCountryCode(String countryCode);

    Optional<CountryEntity> findByCountryName(String name);
}