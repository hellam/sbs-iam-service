package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.app.service.DeviceService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.ratelimit.KeyType;
import ke.shiva.shivacorestarter.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Devices Control")
@RateLimit(capacity = 10, refillTokens = 5, refillDuration = "PT2M", keyType = KeyType.IP)
public class DeviceController {
    private final DeviceService deviceService;
    @Value("${shiva.security.cookies.same-site}")
    private String sameSite;
    @Value("${shiva.security.cookies.secure}")
    private boolean secureCookies;
    @Value("${shiva.security.cookies.http-only}")
    private boolean httpOnlyCookies;

    @PostMapping("/device/init")
    public ResponseEntity<Void> registerDevice(
            HttpServletRequest request,
            HttpServletResponse response,
            @CookieValue(
                    value = SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME,
                    required = false
            ) String deviceIdToken
    ) {
        String deviceId = deviceService.initiateDeviceRegistration(request, deviceIdToken);

        // If same device ID, nothing to set
        if (deviceId.equals(deviceIdToken)) {
            return ResponseEntity.noContent().build();
        }

        // Otherwise issue cookie
        Cookie cookie = new Cookie(SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME, deviceId);
        cookie.setHttpOnly(httpOnlyCookies);
        cookie.setSecure(secureCookies); // MUST be true in banking systems
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 365); // 1 year

        // SameSite must be set manually in servlet stack
        response.addHeader(
                "Set-Cookie",
                String.format(
                        "%s=%s; Max-Age=%d; Path=%s; %s; %s; SameSite=%s",
                        cookie.getName(),
                        cookie.getValue(),
                        cookie.getMaxAge(),
                        cookie.getPath(),
                        cookie.getSecure() ? "Secure" : "",
                        cookie.isHttpOnly() ? "HttpOnly" : "",
                        sameSite
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
