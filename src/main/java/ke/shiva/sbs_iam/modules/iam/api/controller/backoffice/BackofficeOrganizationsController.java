package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeAccessLockRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationRoleCreateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserAccountsUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserAddRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserBasicKycUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserOnboardNonBankRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserRoleUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationUserSearchRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserOnboardResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserSearchResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationRoleResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationRoleDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationRolesPermissionsResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeOrganizationsService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @Operation(summary = "Get organization audit trail", description = "Returns audit events for a company profile")
    @GetMapping("/{clientId}/audit-trail")
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeAuditTrailResponse>>> getOrganizationAuditTrail(
            @PathVariable String clientId,
            HttpServletRequest request
    ) {
        return ResponseBuilder.success(
                "Organization audit trail retrieved",
                backofficeOrganizationsService.getOrganizationAuditTrail(clientId, request)
        );
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

    @Operation(summary = "List organization roles", description = "Returns organization roles for user assignment")
    @GetMapping("/{clientId}/roles")
    public ResponseEntity<ApiResponse<List<BackofficeOrganizationRoleResponse>>> getOrganizationRoles(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Organization roles retrieved",
                backofficeOrganizationsService.getOrganizationRoles(clientId)
        );
    }

    @Operation(summary = "List organization roles with permissions")
    @GetMapping("/{clientId}/roles-permissions")
    public ResponseEntity<ApiResponse<BackofficeOrganizationRolesPermissionsResponse>> getOrganizationRolesPermissions(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Organization roles and permissions retrieved",
                backofficeOrganizationsService.getOrganizationRolesPermissions(clientId)
        );
    }

    @Operation(summary = "Create organization role and link permissions")
    @PostMapping("/{clientId}/roles")
    public ResponseEntity<ApiResponse<BackofficeOrganizationRoleDetailsResponse>> createOrganizationRole(
            @PathVariable String clientId,
            @Valid @RequestBody BackofficeOrganizationRoleCreateRequest request
    ) {
        return ResponseBuilder.success(
                "Organization role created",
                backofficeOrganizationsService.createOrganizationRole(clientId, request)
        );
    }

    @Operation(summary = "Update organization role and linked permissions")
    @PutMapping("/{clientId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<BackofficeOrganizationRoleDetailsResponse>> updateOrganizationRole(
            @PathVariable String clientId,
            @PathVariable Long roleId,
            @Valid @RequestBody BackofficeOrganizationRoleCreateRequest request
    ) {
        return ResponseBuilder.success(
                "Organization role updated",
                backofficeOrganizationsService.updateOrganizationRole(clientId, roleId, request)
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
    @GetMapping("/{clientId}/users/{organizationUserRef}")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> getOrganizationUser(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef
    ) {
        return ResponseBuilder.success(
                "Organization user retrieved",
                backofficeOrganizationsService.getOrganizationUser(clientId, organizationUserRef)
        );
    }

    @Operation(summary = "Block/unblock organization user internet access")
    @PatchMapping("/{clientId}/users/{organizationUserRef}/access-lock")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> updateOrganizationUserAccessLock(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef,
            @Valid @RequestBody BackofficeAccessLockRequest request
    ) {
        return ResponseBuilder.success(
                request.getBlocked() ? "Organization user access blocked" : "Organization user access unblocked",
                backofficeOrganizationsService.updateOrganizationUserAccessLock(
                        clientId,
                        organizationUserRef,
                        request.getBlocked()
                )
        );
    }

    @Operation(summary = "Reset organization user password")
    @PostMapping("/{clientId}/users/{organizationUserRef}/password/reset")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> resetOrganizationUserPassword(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef
    ) {
        return ResponseBuilder.success(
                "Organization user password reset",
                backofficeOrganizationsService.resetOrganizationUserPassword(clientId, organizationUserRef)
        );
    }

    @Operation(summary = "Reset organization user MFA")
    @PostMapping("/{clientId}/users/{organizationUserRef}/mfa/reset")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> resetOrganizationUserMfa(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef
    ) {
        return ResponseBuilder.success(
                "Organization user MFA reset",
                backofficeOrganizationsService.resetOrganizationUserMfa(clientId, organizationUserRef)
        );
    }

    @Operation(summary = "Sync organization user KYC from core banking")
    @PostMapping("/{clientId}/users/{organizationUserRef}/kyc/sync")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> syncOrganizationUserKyc(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef
    ) {
        return ResponseBuilder.success(
                "Organization user KYC synced",
                backofficeOrganizationsService.syncOrganizationUserKyc(clientId, organizationUserRef)
        );
    }

    @Operation(summary = "Update organization user basic KYC for unverified users")
    @PatchMapping("/{clientId}/users/{organizationUserRef}/kyc/basic")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> updateOrganizationUserBasicKyc(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef,
            @Valid @RequestBody BackofficeOrganizationUserBasicKycUpdateRequest request
    ) {
        return ResponseBuilder.success(
                "Organization user basic KYC updated",
                backofficeOrganizationsService.updateOrganizationUserBasicKyc(clientId, organizationUserRef, request)
        );
    }

    @Operation(summary = "Update organization user role")
    @PatchMapping("/{clientId}/users/{organizationUserRef}/role")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> updateOrganizationUserRole(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef,
            @Valid @RequestBody BackofficeOrganizationUserRoleUpdateRequest request
    ) {
        return ResponseBuilder.success(
                "Organization user role updated",
                backofficeOrganizationsService.updateOrganizationUserRole(clientId, organizationUserRef, request)
        );
    }

    @Operation(summary = "Update organization user account access")
    @PutMapping("/{clientId}/users/{organizationUserRef}/accounts")
    public ResponseEntity<ApiResponse<BackofficeOrganizationUserResponse>> updateOrganizationUserAccounts(
            @PathVariable String clientId,
            @PathVariable String organizationUserRef,
            @Valid @RequestBody BackofficeOrganizationUserAccountsUpdateRequest request
    ) {
        return ResponseBuilder.success(
                "Organization user account access updated",
                backofficeOrganizationsService.updateOrganizationUserAccounts(clientId, organizationUserRef, request)
        );
    }
}
