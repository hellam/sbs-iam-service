package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeClientLookupRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeCustomerOnboardingRequest;
import ke.shiva.sbs_iam.modules.iam.domain.enums.backoffice.BackofficeLookupType;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeOrganizationOnboardingRequest;
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

    @Operation(summary = "Lookup Details (Core + IAM validation)", description = "lookupType values: CUSTOMERS, ORGANIZATIONS, EMPLOYEES")
    @PostMapping("/lookup/{lookupType}")
    public ResponseEntity<ApiResponse<BackofficeCustomerLookupResponse>> lookup(
            @PathVariable BackofficeLookupType lookupType,
            @RequestBody @Valid BackofficeClientLookupRequest request) {

        BackofficeCustomerLookupResponse response = switch (lookupType) {
            case CUSTOMERS -> onboardingService.lookupCustomer(request.getClientId());
            case ORGANIZATIONS -> onboardingService.lookupOrganization(request.getClientId());
            case EMPLOYEES -> onboardingService.lookupEmployee(request.getClientId());
        };

        return ResponseBuilder.success(lookupType.successMessage(), response);
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

    @Operation(summary = "Create Organization Onboarding")
    @PostMapping("/organizations")
    public ResponseEntity<ApiResponse<BackofficeOrganizationOnboardingResponse>> createOrganization(
            @RequestBody @Valid BackofficeOrganizationOnboardingRequest request) {
        BackofficeOrganizationOnboardingResponse response = onboardingService.createOrganization(
                BackofficeOnboardingCommand.builder()
                        .clientId(request.getClientId())
                        .isSme(request.getIsSme())
                        .build()
        );
        return ResponseBuilder.success("Organization onboarding successful", response);
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
