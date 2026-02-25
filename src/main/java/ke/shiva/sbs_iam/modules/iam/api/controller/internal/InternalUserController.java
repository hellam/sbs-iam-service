package ke.shiva.sbs_iam.modules.iam.api.controller.internal;

import ke.shiva.client.iam.dto.response.UserPiiResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.IamUserService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Internal API controller for inter-service communication.
 * Provides endpoints for retrieving user PII without exposing it in JWTs.
 *
 * <p>Security:
 * <ul>
 *   <li>Only accessible from Gateway (validates downstream JWT)</li>
 *   <li>Not exposed to external clients</li>
 *   <li>Used by services to fetch user contact info for notifications</li>
 * </ul>
 *
 * @author Shiva Banking Platform
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final IamUserRepository iamUserRepository;
    private final UserContactRepository userContactRepository;
    private final IamUserService iamUserService;

    /**
     * Retrieve user PII by user ID.
     * Used by downstream services to fetch contact information for notifications.
     *
     * @param userId IAM user ID
     * @return user PII response
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPiiResponse>> getUserPii(@PathVariable Long userId) {
        log.debug("Internal API: Fetching user PII for userId={}", userId);

        IamUserEntity user = iamUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for userId={}", userId);
                    return new RuntimeException("User not found");
                });

        // Extract user details from nested entities
        String fullName = user.getParty() != null ? user.getParty().getPerson().getFullName() : "Unknown";
        Map<String, String> contactInfo = iamUserService.getUserPrimaryContactInfo(user);
        String email = contactInfo.get("email");
        String phoneNumber = contactInfo.get("phone");
        String preferredLanguage = "en";
        String category = "CUSTOMER"; // Default
        boolean active = user.getStatus().name().equals("ACTIVE");

        // Determine category from profile
        if (user.getCustomerProfile() != null) {
            category = "CUSTOMER";
        } else if (user.getEmployeeProfile() != null) {
            category = "EMPLOYEE";
        }

        UserPiiResponse response = UserPiiResponse.builder()
                .userId(user.getId())
                .fullName(fullName)
                .email(email)
                .phoneNumber(phoneNumber)
                .preferredLanguage(preferredLanguage)
                .category(category)
                .active(active)
                .build();

        log.debug("Successfully retrieved user PII for userId={}", userId);

        return ResponseBuilder.success("User PII retrieved successfully", response);
    }

    /**
     * Health check endpoint for internal API
     **/
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseBuilder.success("Internal API is healthy", "OK");
    }
}
