package ke.shiva.sbs_iam.modules.iam.api.controller.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.client.iam.dto.request.CustomerRegistrationDetailsRequest;
import ke.shiva.client.iam.dto.request.CustomerRegistrationValidationRequest;
import ke.shiva.client.iam.dto.response.CustomerRegistrationResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.CustomerRegistrationService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("internal/register")
@RequiredArgsConstructor
@Tag(name = "IAM Registration", description = "Customer registration for internet and mobile banking channels")
public class RegistrationController {

    private final CustomerRegistrationService customerRegistrationService;

    @Operation(summary = "Register Customer in IAM", description = "Registers a new customer in IAM using pre-validated core banking details")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerRegistrationResponse>> registerInternetCustomer(
            @RequestBody @Valid CustomerRegistrationDetailsRequest request) {
        log.info("Received IAM registration request for client ID: {}", request.getClientId());
        CustomerRegistrationResponse response = customerRegistrationService.registerCustomer(request);
        return ResponseBuilder.success("Customer registration successful", response);
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Void>> validateInternetCustomer(
            @RequestBody @Valid CustomerRegistrationValidationRequest request) {
        customerRegistrationService.validateInternetCustomer(request);

        return ResponseBuilder.success("Customer validation successful");
    }
}
