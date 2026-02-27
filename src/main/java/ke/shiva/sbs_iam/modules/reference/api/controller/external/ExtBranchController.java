package ke.shiva.sbs_iam.modules.reference.api.controller.external;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ke.shiva.sbs_iam.modules.reference.api.mapper.BranchMapper;
import ke.shiva.sbs_iam.modules.reference.api.response.BranchResponse;
import ke.shiva.sbs_iam.modules.reference.app.service.BranchService;
import ke.shiva.sbs_iam.modules.reference.app.service.CountryService;
import ke.shiva.sbs_iam.modules.reference.domain.entity.BranchEntity;
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
@RequestMapping("/branches")
public class ExtBranchController {

    private final BranchService branchService;
    private final CountryService countryService;

    @GetMapping
    @Operation(summary = "Get all branches with pagination")
    public ResponseEntity<ApiResponse<Page<BranchResponse>>> getAllBranches(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<BranchEntity> branches = branchService.findAll(pageable);
        Page<BranchResponse> response = branches.map(BranchMapper::toPublicResponse);
        return ResponseBuilder.success(response);
    }

    @GetMapping("/code/{branchCode}")
    @Operation(summary = "Get branch by code")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchByCode(@PathVariable String branchCode) {
        BranchEntity branch = branchService.findByBranchCode(branchCode)
                .orElseThrow(() -> BaseException.notFound("Branch not found with code: " + branchCode));
        BranchResponse response = BranchMapper.toPublicResponse(branch);
        return ResponseBuilder.success(response);
    }
}
