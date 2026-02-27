package ke.shiva.sbs_iam.modules.iam.api.controller.internal;

import ke.shiva.client.iam.dto.response.CustomerProfileResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerProfileRepository;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Internal API controller for inter-service communication.
 * Provides endpoints for retrieving a customer profile without exposing it in JWTs.
 *
 * <p>Security:
 * <ul>
 *   <li>Only accessible from Gateway (validates downstream JWT)</li>
 *   <li>Not exposed to external clients</li>
 *   <li>Used by services to fetch customer profile</li>
 * </ul>
 *
 * @author Shiva Banking Platform
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerProfileRepository customerProfileRepository;
    /**
     * Retrieve Customer Profile by core_customer_id
     * */
    @GetMapping({"/{customerId}/profile"})
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getCustomerProfile(@PathVariable String customerId) {
        CustomerProfileEntity customerProfile = customerProfileRepository.findByCoreCustomerId(customerId).orElseThrow(() -> BaseException.notFound("Customer profile not found"));
        return ResponseBuilder.success(CustomerProfileResponse.builder()
                        .iamUserId(customerProfile.getIamUser().getId())
                .coreCustomerId(customerProfile.getCoreCustomerId())
                .segment(customerProfile.getSegment())
                .language(customerProfile.getLanguage())
                .timezone(customerProfile.getTimezone())
                .theme(customerProfile.getTheme())
                .allowEmail(customerProfile.getAllowEmail())
                .allowSms(customerProfile.getAllowSms())
                .allowPush(customerProfile.getAllowPush())
                .createdAt(customerProfile.getCreatedAt().toString())
                .updatedAt(customerProfile.getUpdatedAt().toString())
                .build()
        );
    }
}
