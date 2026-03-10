package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeAccessLockRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserAddRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserBasicKycUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserOnboardNonBankRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserSearchRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserOnboardResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserSearchResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeOrganizationsService;
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
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Tag(name = "Backoffice Organizations", description = "Backoffice organization listing")
public class BackofficeOrganizationsController {

    private final BackofficeOrganizationsService backofficeOrganizationsService;

    @Operation(summary = "List organizations", description = "Returns a paginated organization list for backoffice")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeOrganizationSummaryResponse>>> getOrganizations(
            HttpServletRequest request
    ) {
        return ResponseBuilder.success("Organizations retrieved", backofficeOrganizationsService.getOrganizations(request));
    }

    @Operation(summary = "Block/unblock organization")
    @PatchMapping("/{clientId}/access-lock")
    public ResponseEntity<ApiResponse<BackofficeOrganizationSummaryResponse>> updateOrganizationAccessLock(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeAccessLockRequest request
    ) {
        return ResponseBuilder.success(
                request.getBlocked() ? "Organization access blocked" : "Organization access unblocked",
                backofficeOrganizationsService.updateOrganizationAccountLock(clientId, request.getBlocked())
        );
    }

    @Operation(summary = "Sync organization KYC from core banking")
    @PostMapping("/{clientId}/kyc/sync")
    public ResponseEntity<ApiResponse<BackofficeOrganizationSummaryResponse>> syncOrganizationKyc(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Organization KYC synced",
                backofficeOrganizationsService.syncOrganizationKyc(clientId)
        );
    }

    @Operation(summary = "List organization users", description = "Returns users linked to organization_user for the client ID")
    @GetMapping("/{clientId}/users")
    public ResponseEntity<ApiResponse<List<BackofficeOrganizationUserResponse>>> getOrganizationUsers(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Organization users retrieved",
                backofficeOrganizationsService.getOrganizationUsers(clientId)
        );
    }

    @Operation(summary = "Search existing IAM users for organization linking")
    @PostMapping("/{clientId}/users/search")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserSearchResponse>> searchOrganizationUsers(
            @PathVariable String clientId,
            @RequestBody BackofficeOrganizationUserSearchRequest request
    ) {
        return ResponseBuilder.success(
                "Organization user search completed",
                backofficeOrganizationsService.searchOrganizationUsers(clientId, request)
        );
    }

    @Operation(summary = "Link existing IAM user to organization and selected accounts")
    @PostMapping("/{clientId}/users/link")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> addOrganizationUser(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeOrganizationUserAddRequest request
    ) {
        return ResponseBuilder.success(
                "Organization user linked",
                backofficeOrganizationsService.addOrganizationUser(clientId, request)
        );
    }

    @Operation(summary = "Onboard non-bank organization user and link selected accounts")
    @PostMapping("/{clientId}/users/onboard-non-bank")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserOnboardResponse>> onboardNonBankOrganizationUser(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeOrganizationUserOnboardNonBankRequest request
    ) {
        return ResponseBuilder.success(
                "Non-bank organization user onboarded",
                backofficeOrganizationsService.onboardNonBankOrganizationUser(clientId, request)
        );
    }

    @Operation(summary = "Get organization user")
    @GetMapping("/{clientId}/users/{organizationUserId}")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> getOrganizationUser(
            @PathVariable String clientId,
            @PathVariable Long organizationUserId
    ) {
        return ResponseBuilder.success(
                "Organization user retrieved",
                backofficeOrganizationsService.getOrganizationUser(clientId, organizationUserId)
        );
    }

    @Operation(summary = "Block/unblock organization user internet access")
    @PatchMapping("/{clientId}/users/{organizationUserId}/access-lock")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> updateOrganizationUserAccessLock(
            @PathVariable String clientId,
            @PathVariable Long organizationUserId,
            @Valid @RequestBody BackofficeAccessLockRequest request
    ) {
        return ResponseBuilder.success(
                request.getBlocked() ? "Organization user access blocked" : "Organization user access unblocked",
                backofficeOrganizationsService.updateOrganizationUserAccessLock(
                        clientId,
                        organizationUserId,
                        request.getBlocked()
                )
        );
    }

    @Operation(summary = "Reset organization user password")
    @PostMapping("/{clientId}/users/{organizationUserId}/password/reset")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> resetOrganizationUserPassword(
            @PathVariable String clientId,
            @PathVariable Long organizationUserId
    ) {
        return ResponseBuilder.success(
                "Organization user password reset",
                backofficeOrganizationsService.resetOrganizationUserPassword(clientId, organizationUserId)
        );
    }

    @Operation(summary = "Reset organization user MFA")
    @PostMapping("/{clientId}/users/{organizationUserId}/mfa/reset")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> resetOrganizationUserMfa(
            @PathVariable String clientId,
            @PathVariable Long organizationUserId
    ) {
        return ResponseBuilder.success(
                "Organization user MFA reset",
                backofficeOrganizationsService.resetOrganizationUserMfa(clientId, organizationUserId)
        );
    }

    @Operation(summary = "Sync organization user KYC from core banking")
    @PostMapping("/{clientId}/users/{organizationUserId}/kyc/sync")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> syncOrganizationUserKyc(
            @PathVariable String clientId,
            @PathVariable Long organizationUserId
    ) {
        return ResponseBuilder.success(
                "Organization user KYC synced",
                backofficeOrganizationsService.syncOrganizationUserKyc(clientId, organizationUserId)
        );
    }

    @Operation(summary = "Update organization user basic KYC for unverified users")
    @PatchMapping("/{clientId}/users/{organizationUserId}/kyc/basic")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> updateOrganizationUserBasicKyc(
            @PathVariable String clientId,
            @PathVariable Long organizationUserId,
            @Valid @RequestBody BackofficeOrganizationUserBasicKycUpdateRequest request
    ) {
        return ResponseBuilder.success(
                "Organization user basic KYC updated",
                backofficeOrganizationsService.updateOrganizationUserBasicKyc(clientId, organizationUserId, request)
        );
    }
}
