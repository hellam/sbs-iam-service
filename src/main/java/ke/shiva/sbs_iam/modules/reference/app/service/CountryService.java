package ke.shiva.sbs_iam.modules.reference.app.service;

import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.CountryRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    @Transactional(readOnly = true)
    public Page<CountryEntity> findAll(Pageable pageable) {
        log.debug("Fetching all countries with pagination: {}", pageable);
        return countryRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public CountryEntity findById(Long id) {
        log.debug("Fetching country by id: {}", id);
        return countryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Country not found with id: {}", id);
                    return BaseException.notFound("Country not found with id: " + id);
                });
    }

    @Transactional(readOnly = true)
    public Optional<CountryEntity> findByCountryCode(String countryCode) {
        log.debug("Fetching country by code: {}", countryCode);
        return countryRepository.findByCountryCode(countryCode);
    }

    @Transactional(readOnly = true)
    public Optional<CountryEntity> findByCountryName(String countryName) {
        log.debug("Fetching country by name: {}", countryName);
        return countryRepository.findByCountryNameIgnoreCase(countryName);
    }

    @Transactional
    public CountryEntity create(CountryEntity country) {
        log.debug("Creating new country: {}", country.getCountryCode());

        // Check if country code already exists
        if (countryRepository.findByCountryCode(country.getCountryCode()).isPresent()) {
            log.error("Country with code {} already exists", country.getCountryCode());
            throw BaseException.badRequest("Country with code " + country.getCountryCode() + " already exists");
        }

        CountryEntity savedCountry = countryRepository.save(country);
        log.info("Country created successfully with id: {}", savedCountry.getId());
        return savedCountry;
    }

    @Transactional
    public CountryEntity update(Long id, CountryEntity countryUpdate) {
        log.debug("Updating country with id: {}", id);

        CountryEntity existingCountry = findById(id);

        // Check if country code is being changed and if new code already exists
        if (!existingCountry.getCountryCode().equals(countryUpdate.getCountryCode())) {
            if (countryRepository.findByCountryCode(countryUpdate.getCountryCode()).isPresent()) {
                log.error("Country with code {} already exists", countryUpdate.getCountryCode());
                throw BaseException.badRequest("Country with code " + countryUpdate.getCountryCode() + " already exists");
            }
        }

        existingCountry.setCountryCode(countryUpdate.getCountryCode());
        existingCountry.setPhoneCode(countryUpdate.getPhoneCode());
        existingCountry.setCountryName(countryUpdate.getCountryName());
        existingCountry.setCurrencyCode(countryUpdate.getCurrencyCode());
        existingCountry.setCurrencyName(countryUpdate.getCurrencyName());

        CountryEntity updatedCountry = countryRepository.save(existingCountry);
        log.info("Country updated successfully with id: {}", id);
        return updatedCountry;
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Deleting country with id: {}", id);

        CountryEntity country = findById(id);
        countryRepository.delete(country);

        log.info("Country deleted successfully with id: {}", id);
    }
}
