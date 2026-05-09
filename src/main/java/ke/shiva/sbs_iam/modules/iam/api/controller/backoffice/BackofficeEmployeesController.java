package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeAccessLockRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeIamUserStatusUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeDetailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeEmployeesService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Backoffice Employees", description = "Backoffice employee listing")
public class BackofficeEmployeesController {

    private final BackofficeEmployeesService backofficeEmployeesService;

    @Operation(summary = "List employees", description = "Returns a paginated employee list for backoffice")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeEmployeeSummaryResponse>>> getEmployees(
            HttpServletRequest request
    ) {
        return ResponseBuilder.success("Employees retrieved", backofficeEmployeesService.getEmployees(request));
    }

    @Operation(summary = "Get employee details", description = "Returns backoffice details for one employee by client ID")
    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<BackofficeEmployeeDetailResponse>> getEmployee(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success("Employee retrieved", backofficeEmployeesService.getEmployee(clientId));
    }

    @Operation(summary = "Get employee audit trail", description = "Returns audit events for an employee profile")
    @GetMapping("/{clientId}/audit-trail")
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeAuditTrailResponse>>> getEmployeeAuditTrail(
            @PathVariable String clientId,
            HttpServletRequest request
    ) {
        return ResponseBuilder.success(
                "Employee audit trail retrieved",
                backofficeEmployeesService.getEmployeeAuditTrail(clientId, request)
        );
    }

    @Operation(summary = "Update employee status", description = "Updates IAM status for an employee profile")
    @PatchMapping("/{clientId}/status")
    public ResponseEntity<ApiResponse<BackofficeEmployeeDetailResponse>> updateEmployeeStatus(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeIamUserStatusUpdateRequest request
    ) {
        return ResponseBuilder.success(
                "Employee status updated",
                backofficeEmployeesService.updateEmployeeStatus(clientId, request.getStatus())
        );
    }

    @Operation(summary = "Block/unblock employee access")
    @PatchMapping("/{clientId}/access-lock")
    public ResponseEntity<ApiResponse<BackofficeEmployeeDetailResponse>> updateEmployeeAccessLock(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeAccessLockRequest request
    ) {
        return ResponseBuilder.success(
                request.getBlocked() ? "Employee access blocked" : "Employee access unblocked",
                backofficeEmployeesService.updateEmployeeAccessLock(clientId, request.getBlocked())
        );
    }

    @Operation(summary = "Reset employee password")
    @PostMapping("/{clientId}/password/reset")
    public ResponseEntity<ApiResponse<BackofficeEmployeeDetailResponse>> resetEmployeePassword(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Employee password reset",
                backofficeEmployeesService.resetEmployeePassword(clientId)
        );
    }

    @Operation(summary = "Reset employee MFA")
    @PostMapping("/{clientId}/mfa/reset")
    public ResponseEntity<ApiResponse<BackofficeEmployeeDetailResponse>> resetEmployeeMfa(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Employee MFA reset",
                backofficeEmployeesService.resetEmployeeMfa(clientId)
        );
    }

    @Operation(summary = "Sync employee KYC from core banking")
    @PostMapping("/{clientId}/kyc/sync")
    public ResponseEntity<ApiResponse<BackofficeEmployeeDetailResponse>> syncEmployeeKyc(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Employee KYC synced",
                backofficeEmployeesService.syncEmployeeKyc(clientId)
        );
    }
}
