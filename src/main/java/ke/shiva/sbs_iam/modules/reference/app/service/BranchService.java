package ke.shiva.sbs_iam.modules.reference.app.service;

import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.infra.repository.BranchRepository;
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
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public Page<BranchEntity> findAll(Pageable pageable) {
        log.debug("Fetching all branches with pagination: {}", pageable);
        return branchRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public BranchEntity findById(Long id) {
        log.debug("Fetching branch by id: {}", id);
        return branchRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Branch not found with id: {}", id);
                    return BaseException.notFound("Branch not found with id: " + id);
                });
    }

    @Transactional(readOnly = true)
    public Optional<BranchEntity> findByBranchCode(String branchCode) {
        log.debug("Fetching branch by code: {}", branchCode);
        return branchRepository.findByBranchCode(branchCode);
    }

    @Transactional
    public BranchEntity create(BranchEntity branch) {
        log.debug("Creating new branch: {}", branch.getBranchCode());

        // Check if branch code already exists
        if (branchRepository.findByBranchCode(branch.getBranchCode()).isPresent()) {
            log.error("Branch with code {} already exists", branch.getBranchCode());
            throw BaseException.badRequest("Branch with code " + branch.getBranchCode() + " already exists");
        }

        BranchEntity savedBranch = branchRepository.save(branch);
        log.info("Branch created successfully with id: {}", savedBranch.getId());
        return savedBranch;
    }

    @Transactional
    public BranchEntity update(Long id, BranchEntity branchUpdate) {
        log.debug("Updating branch with id: {}", id);

        BranchEntity existingBranch = findById(id);

        // Check if branch code is being changed and if new code already exists
        if (!existingBranch.getBranchCode().equals(branchUpdate.getBranchCode())) {
            if (branchRepository.findByBranchCode(branchUpdate.getBranchCode()).isPresent()) {
                log.error("Branch with code {} already exists", branchUpdate.getBranchCode());
                throw BaseException.badRequest("Branch with code " + branchUpdate.getBranchCode() + " already exists");
            }
        }

        existingBranch.setBranchCode(branchUpdate.getBranchCode());
        existingBranch.setBranchName(branchUpdate.getBranchName());
        existingBranch.setBranchTypeEnum(branchUpdate.getBranchTypeEnum());
        existingBranch.setCountryCode(branchUpdate.getCountryCode());
        existingBranch.setAddress(branchUpdate.getAddress());
        existingBranch.setCity(branchUpdate.getCity());
        existingBranch.setLongitude(branchUpdate.getLongitude());
        existingBranch.setLatitude(branchUpdate.getLatitude());
        existingBranch.setParentBranch(branchUpdate.getParentBranch());
        existingBranch.setUpdatedBy(branchUpdate.getUpdatedBy());

        BranchEntity updatedBranch = branchRepository.save(existingBranch);
        log.info("Branch updated successfully with id: {}", id);
        return updatedBranch;
    }

    @Transactional
    public void delete(Long id) {
        log.debug("Deleting branch with id: {}", id);

        BranchEntity branch = findById(id);
        branchRepository.delete(branch);

        log.info("Branch deleted successfully with id: {}", id);
    }
}
