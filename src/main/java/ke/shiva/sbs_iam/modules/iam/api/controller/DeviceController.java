package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.dto.device.DeviceRegistrationRequest;
import ke.shiva.sbs_iam.modules.iam.app.service.DeviceService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.ratelimit.KeyType;
import ke.shiva.shivacorestarter.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Devices Control")
@RateLimit(capacity = 10, refillTokens = 5, refillDuration = "PT2M", keyType = KeyType.IP)
public class DeviceController {
    private final DeviceService deviceService;

    /**
     * Device registration endpoint called by API Gateway.
     * This endpoint receives device information and registers/updates it in the database.
     */
    @PostMapping("/device/init")
    public ResponseEntity<ApiResponse<Void>> registerDeviceFromGateway(
            @Valid @RequestBody DeviceRegistrationRequest request
    ) {
        log.debug("Received device registration from gateway: deviceId={}", request.getDeviceId());
        deviceService.registerDeviceFromGateway(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Device registered successfully", null, null));
    }

}
