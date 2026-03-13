package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSupportContentStatusUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSupportContentUpsertRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeSupportContentResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeSupportContentService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/settings/support-content")
@RequiredArgsConstructor
@Tag(name = "Backoffice Support Content", description = "Backoffice APIs for Help & Support content")
public class BackofficeSupportContentController {

    private final BackofficeSupportContentService backofficeSupportContentService;

    @GetMapping
    @Operation(summary = "List support content by category")
    public ResponseEntity<ApiResponse<List<BackofficeSupportContentResponse>>> listSupportContent(
            @RequestParam String category
    ) {
        return ResponseBuilder.success(backofficeSupportContentService.listByCategory(category));
    }

    @PostMapping
    @Operation(summary = "Create support content entry")
    public ResponseEntity<ApiResponse<BackofficeSupportContentResponse>> createSupportContent(
            @Valid @RequestBody BackofficeSupportContentUpsertRequest request
    ) {
        return ResponseBuilder.success(
                "Support content created successfully.",
                backofficeSupportContentService.create(request)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update support content entry")
    public ResponseEntity<ApiResponse<BackofficeSupportContentResponse>> updateSupportContent(
            @PathVariable String id,
            @Valid @RequestBody BackofficeSupportContentUpsertRequest request
    ) {
        return ResponseBuilder.success(
                "Support content updated successfully.",
                backofficeSupportContentService.update(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate support content entry")
    public ResponseEntity<ApiResponse<BackofficeSupportContentResponse>> updateSupportContentStatus(
            @PathVariable String id,
            @Valid @RequestBody BackofficeSupportContentStatusUpdateRequest request
    ) {
        return ResponseBuilder.success(
                "Support content status updated successfully.",
                backofficeSupportContentService.updateStatus(id, Boolean.TRUE.equals(request.getIsActive()))
        );
    }
}
