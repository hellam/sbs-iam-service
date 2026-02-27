package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.*;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationOnboardingResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeOnboardingService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/backoffice/onboarding")
@RequiredArgsConstructor
@Tag(name = "Backoffice Onboarding", description = "Customer, organization and employee onboarding")
public class BackofficeOnboardingController {

    private final BackofficeOnboardingService onboardingService;

    @Operation(summary = "Validate Customer Onboarding")
    @PostMapping("/customers/validate")
    public ResponseEntity<ApiResponse<Void>> validateCustomer(
            @RequestBody @Valid BackofficeCustomerValidationRequest request) {
        onboardingService.validateCustomer(request);
        return ResponseBuilder.success("Customer validation successful");
    }

    @Operation(summary = "Create Customer Onboarding")
    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<BackofficeCustomerOnboardingResponse>> createCustomer(
            @RequestBody @Valid BackofficeCustomerOnboardingRequest request) {
        BackofficeCustomerOnboardingResponse response = onboardingService.createCustomer(request);
        return ResponseBuilder.success("Customer onboarding successful", response);
    }

    @Operation(summary = "Validate Organization Onboarding")
    @PostMapping("/organizations/validate")
    public ResponseEntity<ApiResponse<Void>> validateOrganization(
            @RequestBody @Valid BackofficeOrganizationValidationRequest request) {
        onboardingService.validateOrganization(request);
        return ResponseBuilder.success("Organization validation successful");
    }

    @Operation(summary = "Create Organization Onboarding")
    @PostMapping("/organizations")
    public ResponseEntity<ApiResponse<BackofficeOrganizationOnboardingResponse>> createOrganization(
            @RequestBody @Valid BackofficeOrganizationOnboardingRequest request) {
        BackofficeOrganizationOnboardingResponse response = onboardingService.createOrganization(request);
        return ResponseBuilder.success("Organization onboarding successful", response);
    }

    @Operation(summary = "Validate Employee Onboarding")
    @PostMapping("/employees/validate")
    public ResponseEntity<ApiResponse<Void>> validateEmployee(
            @RequestBody @Valid BackofficeEmployeeValidationRequest request) {
        onboardingService.validateEmployee(request);
        return ResponseBuilder.success("Employee validation successful");
    }

    @Operation(summary = "Create Employee Onboarding")
    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<BackofficeEmployeeOnboardingResponse>> createEmployee(
            @RequestBody @Valid BackofficeEmployeeOnboardingRequest request) {
        BackofficeEmployeeOnboardingResponse response = onboardingService.createEmployee(request);
        return ResponseBuilder.success("Employee onboarding successful", response);
    }
}
