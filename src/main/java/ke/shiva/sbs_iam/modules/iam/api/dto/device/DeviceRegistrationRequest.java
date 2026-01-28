package ke.shiva.sbs_iam.modules.iam.api.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for device registration request from API Gateway.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {
    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "IP address is required")
    private String ipAddress;

    @NotBlank(message = "User agent hash is required")
    private String userAgentHash;

    private String deviceType;
    private String platform;
    private String browser;
    private String browserVersion;
}
