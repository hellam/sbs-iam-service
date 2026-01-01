package ke.shiva.sbs_iam.modules.iam.app.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Service for IP geolocation using MaxMind GeoIP2 database
 */
@Slf4j
@Service
public class GeoIpService {

    @Value("${geoip.database.path:#{null}}")
    private String databasePath;

    private DatabaseReader databaseReader;

    @Getter
    @Setter
    public static class GeoLocation {
        private String country;
        private String countryCode;
        private String city;
        private String region;
        private Double latitude;
        private Double longitude;
        private String postalCode;
        private String timeZone;
    }

    @PostConstruct
    public void init() {
        if (databasePath != null && !databasePath.isEmpty()) {
            try {
                File database = new File(databasePath);
                if (database.exists()) {
                    databaseReader = new DatabaseReader.Builder(database).build();
                    log.info("GeoIP database loaded successfully from: {}", databasePath);
                } else {
                    log.warn("GeoIP database file not found at: {}. GeoIP lookups will be disabled.", databasePath);
                }
            } catch (IOException e) {
                log.error("Failed to load GeoIP database: {}", e.getMessage());
            }
        } else {
            log.info("GeoIP database path not configured. GeoIP lookups will be disabled. " +
                    "Set 'geoip.database.path' property to enable.");
        }
    }

    /**
     * Lookup geographic location by IP address
     *
     * @param ipAddress IP address to lookup
     * @return GeoLocation object or null if lookup fails
     */
    public GeoLocation lookup(String ipAddress) {
        if (databaseReader == null) {
            log.debug("GeoIP lookup skipped - database not available");
            return null;
        }

        if (ipAddress == null || ipAddress.isEmpty()) {
            return null;
        }

        // Skip private/local IP addresses
        if (isPrivateOrLocalIp(ipAddress)) {
            log.debug("GeoIP lookup skipped for private/local IP: {}", ipAddress);
            return null;
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            CityResponse response = databaseReader.city(inetAddress);

            GeoLocation location = getGeoLocation(response);

            log.debug("GeoIP lookup successful for IP {}: {}, {}",
                ipAddress, location.getCity(), location.getCountry());

            return location;

        } catch (IOException | GeoIp2Exception e) {
            log.debug("GeoIP lookup failed for IP {}: {}", ipAddress, e.getMessage());
            return null;
        }
    }

    private static @NonNull GeoLocation getGeoLocation(CityResponse response) {
        GeoLocation location = new GeoLocation();

        // Country information
        if (response.getCountry() != null) {
            location.setCountry(response.getCountry().getName());
            location.setCountryCode(response.getCountry().getIsoCode());
        }

        // City information
        if (response.getCity() != null) {
            location.setCity(response.getCity().getName());
        }

        // Region/State information
        if (response.getMostSpecificSubdivision() != null) {
            location.setRegion(response.getMostSpecificSubdivision().getName());
        }

        // Location coordinates
        if (response.getLocation() != null) {
            location.setLatitude(response.getLocation().getLatitude());
            location.setLongitude(response.getLocation().getLongitude());
            location.setTimeZone(response.getLocation().getTimeZone());
        }

        // Postal code
        if (response.getPostal() != null) {
            location.setPostalCode(response.getPostal().getCode());
        }
        return location;
    }

    /**
     * Check if IP address is private or local
     */
    private boolean isPrivateOrLocalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }

        // Check for localhost
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("localhost")) {
            return true;
        }

        // Check for private IP ranges
        try {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                int firstOctet = Integer.parseInt(parts[0]);
                int secondOctet = Integer.parseInt(parts[1]);

                // 10.0.0.0 - 10.255.255.255
                if (firstOctet == 10) {
                    return true;
                }

                // 172.16.0.0 - 172.31.255.255
                if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
                    return true;
                }

                // 192.168.0.0 - 192.168.255.255
                if (firstOctet == 192 && secondOctet == 168) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            // Not a valid IPv4 address, might be IPv6
            return false;
        }

        return false;
    }
}

