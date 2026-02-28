package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeCustomerOnboardingRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeClientLookupRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerAccountResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerLookupResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeOnboardingService;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.dto.BackofficeOnboardingCommand;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
@Tag(name = "Backoffice Onboarding", description = "Customer, organization and employee onboarding")
public class BackofficeOnboardingController {

    private final BackofficeOnboardingService onboardingService;

    @Operation(summary = "Lookup Customer Details (Core + IAM validation)")
    @PostMapping("/customers/lookup")
    public ResponseEntity<ApiResponse<BackofficeCustomerLookupResponse>> lookupCustomer(
            @RequestBody @Valid BackofficeClientLookupRequest request) {
        return ResponseBuilder.success("Customer lookup successful",
                onboardingService.lookupCustomer(request.getClientId()));
    }

    @Operation(summary = "Lookup Customer Accounts (Individual clients only)")
    @GetMapping("/customers/{clientId}/accounts")
    public ResponseEntity<ApiResponse<List<BackofficeCustomerAccountResponse>>> lookupCustomerAccounts(
            @PathVariable String clientId,
            @RequestParam(name = "q", required = false) String query
    ) {
        List<BackofficeCustomerAccountResponse> response =
                onboardingService.lookupCustomerAccounts(clientId, query);
        return ResponseBuilder.success("Customer accounts retrieved", response);
    }

    @Operation(summary = "Create Customer Onboarding")
    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<BackofficeCustomerOnboardingResponse>> createCustomer(
            @RequestBody @Valid BackofficeCustomerOnboardingRequest request) {
        BackofficeCustomerOnboardingResponse response = onboardingService.createCustomer(
                BackofficeOnboardingCommand.builder()
                        .clientId(request.getClientId())
                        .accounts(request.getAccounts())
                        .build()
        );
        return ResponseBuilder.success("Customer onboarding successful", response);
    }

    @Operation(summary = "Lookup Organization Details (Core + IAM validation)")
    @PostMapping("/organizations/lookup")
    public ResponseEntity<ApiResponse<BackofficeCustomerLookupResponse>> lookupOrganization(
            @RequestBody @Valid BackofficeClientLookupRequest request) {
        return ResponseBuilder.success("Organization lookup successful",
                onboardingService.lookupOrganization(request.getClientId()));
    }

    @Operation(summary = "Create Organization Onboarding")
    @PostMapping("/organizations")
    public ResponseEntity<ApiResponse<BackofficeOrganizationOnboardingResponse>> createOrganization(
            @RequestBody @Valid BackofficeClientLookupRequest request) {
        BackofficeOrganizationOnboardingResponse response = onboardingService.createOrganization(
                BackofficeOnboardingCommand.builder()
                        .clientId(request.getClientId())
                        .build()
        );
        return ResponseBuilder.success("Organization onboarding successful", response);
    }

    @Operation(summary = "Lookup Employee Details (Core + IAM validation)")
    @PostMapping("/employees/lookup")
    public ResponseEntity<ApiResponse<BackofficeCustomerLookupResponse>> lookupEmployee(
            @RequestBody @Valid BackofficeClientLookupRequest request) {
        return ResponseBuilder.success("Employee lookup successful",
                onboardingService.lookupEmployee(request.getClientId()));
    }

    @Operation(summary = "Create Employee Onboarding")
    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<BackofficeEmployeeOnboardingResponse>> createEmployee(
            @RequestBody @Valid BackofficeClientLookupRequest request) {
        BackofficeEmployeeOnboardingResponse response = onboardingService.createEmployee(
                BackofficeOnboardingCommand.builder()
                        .clientId(request.getClientId())
                        .build()
        );
        return ResponseBuilder.success("Employee onboarding successful", response);
    }
}
