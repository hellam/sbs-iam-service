package ke.shiva.sbs_iam.modules.iam.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating development JWT tokens.
 * All fields are optional and will use sensible defaults if not provided.
 *
 * @author Shiva Banking Platform
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevTokenRequest {

    /**
     * IAM user ID.
     * Default: 1
     */
    private Long userId;

    /**
     * Core banking customer ID.
     * Default: "DEV_CUSTOMER"
     */
    private String customerId;

    /**
     * Session ID for tracking.
     * Default: generated UUID
     */
    private String sessionId;

    /**
     * Device ID for device binding.
     * Default: "DEV_DEVICE"
     */
    private String deviceId;

    /**
     * Authentication channel.
     * Default: "INTERNET_BANKING"
     * Valid values: INTERNET_BANKING, MOBILE_BANKING, BACKOFFICE, API
     */
    private String channel;

    /**
     * User category.
     * Default: "CUSTOMER"
     * Valid values: CUSTOMER, EMPLOYEE, ADMIN, SYSTEM
     */
    private String category;

    /**
     * Token expiry in seconds.
     * Default: 3600 (1 hour)
     * Min: 60, Max: 86400 (24 hours)
     */
    private Long expirySeconds;

    /**
     * Profile type (optional).
     * Example: "PERSONAL", "BUSINESS"
     */
    private String profileType;

    /**
     * Profile ID (optional).
     */
    private Long profileId;

    /**
     * OAuth scope (optional).
     * Default: derived from channel
     */
    private String scope;
}
