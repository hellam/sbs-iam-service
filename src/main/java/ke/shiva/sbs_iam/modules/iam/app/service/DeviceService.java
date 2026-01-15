package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.DeviceEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.DeviceRepository;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.RequestUtil;
import ke.shiva.shivacorestarter.util.SecureRandomStringGen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final GeoIpService geoIpService;

    public String initiateDeviceRegistration(HttpServletRequest request, String deviceIdToken) {
        String ipAddress = RequestUtil.getClientIp(request);
        String userAgentHash = HashUtil.sha256(request.getHeader(SecurityConstants.Headers.USER_AGENT_HEADER));

        if (deviceIdToken != null && !deviceIdToken.isEmpty()) {
            DeviceEntity existingDevice = deviceRepository.findByDeviceIdAndActiveTrue(
                    HashUtil.sha256(deviceIdToken)
            ).orElse(null);

            if (existingDevice != null) {
                updateLastSeen(existingDevice, ipAddress, userAgentHash);
                return deviceIdToken;
            }
        }

        String deviceId = SecureRandomStringGen.generateHex(16);
        createDevice(request, userAgentHash, deviceId, ipAddress);
        return deviceId;
    }

    private void createDevice(HttpServletRequest request, String userAgentHash, String deviceId, String ipAddress) {
        GeoIpService.GeoLocation location = geoIpService.lookup(ipAddress);

        DeviceEntity device = new DeviceEntity();
        device.setDeviceId(HashUtil.sha256(deviceId));
        device.setDeviceType(request.getHeader(SecurityConstants.Headers.DEVICE_TYPE));
        device.setPlatform(request.getHeader(SecurityConstants.Headers.PLATFORM));
        device.setBrowser(request.getHeader(SecurityConstants.Headers.BROWSER));
        device.setBrowserVersion(request.getHeader(SecurityConstants.Headers.BROWSER_VERSION));
        device.setUserAgentHash(userAgentHash);
        device.setFirstIp(ipAddress);
        device.setFirstSeenAt(java.time.Instant.now());
        device.setFirstCountry(location != null && location.getCountry() != null ? location.getCountry() : null);
        device.setFirstCountry(location != null && location.getCity() != null ? location.getCity() : null);
        device.setLastCountry(location != null && location.getCountry() != null ? location.getCountry() : null);
        device.setLastCity(location != null && location.getCity() != null ? location.getCity() : null);
        device.setLastSeenAt(java.time.Instant.now());
        device.setLastIp(ipAddress);
        device.setActive(true);

        device.setCreatedAt(OffsetDateTime.now());
        device.setUpdatedAt(OffsetDateTime.now());
        deviceRepository.save(device);
    }

    private void updateLastSeen(DeviceEntity device, String ipAddress, String userAgentHash) {
        GeoIpService.GeoLocation location = geoIpService.lookup(ipAddress);

        device.setLastCountry(location != null && location.getCountry() != null ? location.getCountry() : null);
        device.setLastCity(location != null && location.getCity() != null ? location.getCity() : null);
        device.setLastSeenAt(java.time.Instant.now());
        device.setLastIp(ipAddress);
        device.setUserAgentHash(userAgentHash);

        device.setUpdatedAt(OffsetDateTime.now());
        deviceRepository.save(device);
    }
}
