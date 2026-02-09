package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.IamRegistrationDetailsRequest;
import ke.shiva.sbs_iam.modules.iam.app.service.CustomerRegistrationService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
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
@RequestMapping("/iam/register")
@RequiredArgsConstructor
@Tag(name = "IAM Registration", description = "Customer registration for internet banking")
public class RegistrationController {

    private final CustomerRegistrationService customerRegistrationService;

    @Operation(summary = "Register Customer in IAM", description = "Registers a new customer in IAM using pre-validated core banking details")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerInternetCustomer(
            @RequestBody @Valid IamRegistrationDetailsRequest request) {
        log.info("Received IAM registration request for client ID: {}", request.getClientDetails());
        request.setChannel(Channel.INTERNET_BANKING);
        return ResponseBuilder.success(customerRegistrationService.registerCustomer(request));
    }
}
