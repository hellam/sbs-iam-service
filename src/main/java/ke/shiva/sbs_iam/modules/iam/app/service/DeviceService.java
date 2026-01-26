package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.dto.device.DeviceRegistrationRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.DeviceEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final GeoIpService geoIpService;

    /**
     * Register or update device from API Gateway.
     * This method is called by the gateway for all requests.
     */
    public void registerDeviceFromGateway(DeviceRegistrationRequest request) {
        // Check if device already exists
        DeviceEntity existingDevice = deviceRepository.findByDeviceIdAndActiveTrue(
                request.getDeviceId()
        ).orElse(null);

        if (existingDevice != null) {
            // Update last seen information
            updateDeviceLastSeen(existingDevice, request);
        } else {
            // Create new device entry
            createDeviceFromGateway(request);
        }
    }

    /**
     * Create new device entry from gateway registration request.
     */
    private void createDeviceFromGateway(DeviceRegistrationRequest request) {
        GeoIpService.GeoLocation location = geoIpService.lookup(request.getIpAddress());

        DeviceEntity device = new DeviceEntity();
        device.setDeviceId(request.getDeviceId());
        device.setDeviceType(request.getDeviceType());
        device.setPlatform(request.getPlatform());
        device.setBrowser(request.getBrowser());
        device.setBrowserVersion(request.getBrowserVersion());
        device.setUserAgentHash(request.getUserAgentHash());
        device.setFirstIp(request.getIpAddress());
        device.setFirstSeenAt(java.time.Instant.now());
        device.setFirstCountry(location != null && location.getCountry() != null ? location.getCountry() : null);
        device.setFirstCity(location != null && location.getCity() != null ? location.getCity() : null);
        device.setLastCountry(location != null && location.getCountry() != null ? location.getCountry() : null);
        device.setLastCity(location != null && location.getCity() != null ? location.getCity() : null);
        device.setLastSeenAt(java.time.Instant.now());
        device.setLastIp(request.getIpAddress());
        device.setActive(true);

        device.setCreatedAt(OffsetDateTime.now());
        device.setUpdatedAt(OffsetDateTime.now());
        deviceRepository.save(device);
    }

    /**
     * Update device last seen information from gateway registration request.
     */
    private void updateDeviceLastSeen(DeviceEntity device, DeviceRegistrationRequest request) {
        GeoIpService.GeoLocation location = geoIpService.lookup(request.getIpAddress());

        device.setLastCountry(location != null && location.getCountry() != null ? location.getCountry() : null);
        device.setLastCity(location != null && location.getCity() != null ? location.getCity() : null);
        device.setLastSeenAt(java.time.Instant.now());
        device.setLastIp(request.getIpAddress());
        device.setUserAgentHash(request.getUserAgentHash());

        device.setUpdatedAt(OffsetDateTime.now());
        deviceRepository.save(device);
    }
}
