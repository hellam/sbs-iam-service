package ke.shiva.sbs_iam.modules.reference.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.reference.api.mapper.BranchMapper;
import ke.shiva.sbs_iam.modules.reference.api.request.BranchRequest;
import ke.shiva.sbs_iam.modules.reference.api.response.BranchResponse;
import ke.shiva.sbs_iam.modules.reference.app.service.BranchService;
import ke.shiva.sbs_iam.modules.reference.app.service.CountryService;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
import ke.shiva.sbs_iam.modules.reference.domain.entity.CountryEntity;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Branch Management")
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;
    private final CountryService countryService;

    @GetMapping
    @Operation(summary = "Get all branches with pagination")
    public ResponseEntity<ApiResponse<Page<BranchResponse>>> getAllBranches(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("GET /api/branches - Fetching branches with pagination");
        Page<BranchEntity> branches = branchService.findAll(pageable);
        Page<BranchResponse> response = branches.map(BranchMapper::toResponse);
        return ResponseBuilder.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get branch by ID")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable Long id) {
        log.debug("GET /api/branches/{} - Fetching branch", id);
        BranchEntity branch = branchService.findById(id);
        BranchResponse response = BranchMapper.toResponse(branch);
        return ResponseBuilder.success(response);
    }

    @GetMapping("/code/{branchCode}")
    @Operation(summary = "Get branch by code")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchByCode(@PathVariable String branchCode) {
        log.debug("GET /api/branches/code/{} - Fetching branch", branchCode);
        BranchEntity branch = branchService.findByBranchCode(branchCode)
                .orElseThrow(() -> BaseException.notFound("Branch not found with code: " + branchCode));
        BranchResponse response = BranchMapper.toResponse(branch);
        return ResponseBuilder.success(response);
    }

    @PostMapping
    @Operation(summary = "Create a new branch")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(@Valid @RequestBody BranchRequest request) {
        log.debug("POST /api/branches - Creating branch: {}", request.getBranchCode());

        CountryEntity country = countryService.findByCountryCode(request.getCountryCode())
                .orElseThrow(() -> BaseException.notFound("Country not found with code: " + request.getCountryCode()));

        BranchEntity parentBranch = null;
        if (request.getParentBranchId() != null) {
            parentBranch = branchService.findById(request.getParentBranchId());
        }

        BranchEntity branch = BranchMapper.toEntity(request, country, parentBranch);
        BranchEntity createdBranch = branchService.create(branch);
        BranchResponse response = BranchMapper.toResponse(createdBranch);

        return ResponseBuilder.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing branch")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequest request
    ) {
        log.debug("PUT /api/branches/{} - Updating branch", id);

        CountryEntity country = countryService.findByCountryCode(request.getCountryCode())
                .orElseThrow(() -> BaseException.notFound("Country not found with code: " + request.getCountryCode()));

        BranchEntity parentBranch = null;
        if (request.getParentBranchId() != null) {
            parentBranch = branchService.findById(request.getParentBranchId());
        }

        BranchEntity branch = BranchMapper.toEntity(request, country, parentBranch);
        BranchEntity updatedBranch = branchService.update(id, branch);
        BranchResponse response = BranchMapper.toResponse(updatedBranch);

        return ResponseBuilder.success(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a branch")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long id) {
        log.debug("DELETE /api/branches/{} - Deleting branch", id);
        branchService.delete(id);
        return ResponseBuilder.success("Branch deleted successfully");
    }
}
