package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeAccessLockRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeIamUserStatusUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerDetailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeCustomersService;
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
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Backoffice Customers", description = "Backoffice customer listing")
public class BackofficeCustomersController {

    private final BackofficeCustomersService backofficeCustomersService;

    @Operation(summary = "List customers", description = "Returns a paginated customer list for backoffice")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeCustomerSummaryResponse>>> getCustomers(
            HttpServletRequest request
    ) {
        return ResponseBuilder.success("Customers retrieved", backofficeCustomersService.getCustomers(request));
    }

    @Operation(summary = "Get customer details", description = "Returns backoffice details for one customer by client ID")
    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<BackofficeCustomerDetailResponse>> getCustomer(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success("Customer retrieved", backofficeCustomersService.getCustomer(clientId));
    }

    @Operation(summary = "Get customer audit trail", description = "Returns audit events for a customer profile")
    @GetMapping("/{clientId}/audit-trail")
    public ResponseEntity<ApiResponse<List<BackofficeAuditTrailResponse>>> getCustomerAuditTrail(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Customer audit trail retrieved",
                backofficeCustomersService.getCustomerAuditTrail(clientId)
        );
    }

    @Operation(summary = "Update customer status", description = "Updates IAM status for a customer profile")
    @PatchMapping("/{clientId}/status")
    public ResponseEntity<ApiResponse<BackofficeCustomerDetailResponse>> updateCustomerStatus(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeIamUserStatusUpdateRequest request
    ) {
        return ResponseBuilder.success(
                "Customer status updated",
                backofficeCustomersService.updateCustomerStatus(clientId, request.getStatus())
        );
    }

    @Operation(summary = "Block/unblock customer internet access")
    @PatchMapping("/{clientId}/access-lock")
    public ResponseEntity<ApiResponse<BackofficeCustomerDetailResponse>> updateCustomerAccessLock(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeAccessLockRequest request
    ) {
        return ResponseBuilder.success(
                request.getBlocked() ? "Customer access blocked" : "Customer access unblocked",
                backofficeCustomersService.updateCustomerAccessLock(clientId, request.getBlocked())
        );
    }

    @Operation(summary = "Reset customer password")
    @PostMapping("/{clientId}/password/reset")
    public ResponseEntity<ApiResponse<BackofficeCustomerDetailResponse>> resetCustomerPassword(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Customer password reset",
                backofficeCustomersService.resetCustomerPassword(clientId)
        );
    }

    @Operation(summary = "Reset customer MFA")
    @PostMapping("/{clientId}/mfa/reset")
    public ResponseEntity<ApiResponse<BackofficeCustomerDetailResponse>> resetCustomerMfa(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Customer MFA reset",
                backofficeCustomersService.resetCustomerMfa(clientId)
        );
    }

    @Operation(summary = "Sync customer KYC from core banking")
    @PostMapping("/{clientId}/kyc/sync")
    public ResponseEntity<ApiResponse<BackofficeCustomerDetailResponse>> syncCustomerKyc(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Customer KYC synced",
                backofficeCustomersService.syncCustomerKyc(clientId)
        );
    }
}
