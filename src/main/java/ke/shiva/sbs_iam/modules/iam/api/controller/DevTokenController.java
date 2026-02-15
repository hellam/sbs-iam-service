package ke.shiva.sbs_iam.modules.iam.api.controller;

import ke.shiva.sbs_iam.modules.iam.api.request.DevTokenRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.DevTokenResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.DevTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Development-only controller for generating test JWT tokens.
 * Allows developers to easily create tokens with custom claims and expiry times.
 *
 * <p><strong>WARNING:</strong> This endpoint is ONLY available in 'dev' profile.
 * It should NEVER be enabled in production environments.
 *
 * <p>Example usage:
 * <pre>
 * POST /api/dev/token
 * {
 *   "userId": 123,
 *   "customerId": "CUST001",
 *   "sessionId": "sess_abc123",
 *   "deviceId": "dev_xyz789",
 *   "channel": "MOBILE_BANKING",
 *   "category": "CUSTOMER",
 *   "expirySeconds": 3600
 * }
 * </pre>
 *
 * @author Shiva Banking Platform
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/token")
@Profile("dev")
@RequiredArgsConstructor
public class DevTokenController {

    private final DevTokenService devTokenService;

    /**
     * Generate a development JWT token with custom claims.
     * All parameters are optional and will use defaults if not provided.
     *
     * @param request the token generation request
     * @return JWT token with metadata
     */
    @PostMapping
    public ResponseEntity<DevTokenResponse> generateDevToken(@RequestBody(required = false) DevTokenRequest request) {
        log.warn("DEV TOKEN GENERATION REQUESTED - This should NEVER happen in production!");

        if (request == null) {
            request = new DevTokenRequest();
        }

        DevTokenResponse response = devTokenService.generateDevToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate a simple development token with default values.
     * Convenient for quick testing without specifying any parameters.
     *
     * @return JWT token with default claims
     */
    @GetMapping("/quick")
    public ResponseEntity<DevTokenResponse> generateQuickToken() {
        log.warn("QUICK DEV TOKEN GENERATION REQUESTED - This should NEVER happen in production!");

        DevTokenResponse response = devTokenService.generateDevToken(new DevTokenRequest());
        return ResponseEntity.ok(response);
    }

    /**
     * Get information about available token generation options.
     *
     * @return help information
     */
    @GetMapping("/help")
    public ResponseEntity<String> getHelp() {
        String help = """
                Development Token Generator
                ===========================

                POST /api/dev/token - Generate custom token
                GET /api/dev/token/quick - Generate default token
                GET /api/dev/token/help - Show this help

                Request Body (all optional):
                {
                  "userId": 123,              // Default: 1
                  "customerId": "CUST001",    // Default: "DEV_CUSTOMER"
                  "sessionId": "sess_abc123", // Default: generated UUID
                  "deviceId": "dev_xyz789",   // Default: "DEV_DEVICE"
                  "channel": "MOBILE_BANKING",// Default: "INTERNET_BANKING"
                  "category": "CUSTOMER",     // Default: "CUSTOMER"
                  "expirySeconds": 3600       // Default: 3600 (1 hour)
                }

                Available Channels:
                - INTERNET_BANKING
                - MOBILE_BANKING
                - BACKOFFICE
                - API

                Available Categories:
                - CUSTOMER
                - EMPLOYEE
                - ADMIN
                - SYSTEM

                WARNING: This endpoint is only available in 'dev' profile.
                """;

        return ResponseEntity.ok(help);
    }
}
