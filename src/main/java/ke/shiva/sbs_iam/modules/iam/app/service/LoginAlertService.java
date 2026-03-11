package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.app.util.RequestContextExtractor;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.DeviceEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.infra.external.NotificationService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.DeviceRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAlertService {

    private static final String ALERT_SENT_AT = "loginAlertSentAt";
    private static final String ALERT_SENT_MODE = "loginAlertSentMode";
    private static final String UNKNOWN = "Unknown";

    private final NotificationService notificationService;
    private final UserContactRepository userContactRepository;
    private final DeviceRepository deviceRepository;
    private final GeoIpService geoIpService;
    private final RequestContextExtractor requestContextExtractor;
    private final LoginFlowService loginFlowService;

    public void sendNewLoginAlert(SessionEntity session, String identifier) {
        if (session == null || session.getIamUser() == null) {
            return;
        }

        Long userId = session.getIamUser().getId();

        // Best-effort by design: login must not fail because alerting failed.
        try {
            if (isAlertAlreadySent(session)) {
                log.debug("Login alert already sent for sessionId={}", session.getSessionId());
                return;
            }

            IamUserEntity user = session.getIamUser();
            String email = userContactRepository
                    .findByIamUserAndContactTypeAndPrimaryIsTrue(user, ContactType.EMAIL)
                    .map(UserContact::getContactValue)
                    .orElse(null);

            if (email == null || email.isBlank()) {
                log.warn("Skipping login alert for userId={} because primary email is missing", userId);
                markAlertSentSafely(session, "SKIPPED_NO_EMAIL");
                return;
            }

            AlertContext alertContext = buildAlertContext(session, identifier);

            try {
                notificationService.sendLoginAlertEmail(
                        email,
                        alertContext.userName(),
                        alertContext.deviceType(),
                        alertContext.browser(),
                        alertContext.location(),
                        alertContext.ipAddress(),
                        alertContext.channel(),
                        alertContext.loginTime()
                );
                markAlertSentSafely(session, "TEMPLATE");
            } catch (Exception fallbackError) {
                log.warn("Template-based login alert failed for userId={}, falling back to direct alert: {}",
                        userId, fallbackError.getMessage());
                try {
                    notificationService.sendLoginAlertDirectEmail(
                            email,
                            alertContext.userName(),
                            alertContext.deviceType(),
                            alertContext.browser(),
                            alertContext.location(),
                            alertContext.ipAddress(),
                            alertContext.channel(),
                            alertContext.loginTime()
                    );
                    markAlertSentSafely(session, "DIRECT_FALLBACK");
                } catch (Exception directError) {
                    log.error("Direct login alert fallback also failed for userId={}: {}",
                            userId, directError.getMessage(), directError);
                    markAlertSentSafely(session, "FAILED");
                }
            }
        } catch (Exception e) {
            log.error("Login alert processing failed for userId={} (non-blocking): {}", userId, e.getMessage(), e);
        }
    }

    private AlertContext buildAlertContext(SessionEntity session, String identifier) {
        RequestContextExtractor.RequestContext requestContext = requestContextExtractor.extractContext();
        DeviceEntity device = resolveDevice(session.getDeviceId()).orElse(null);

        String ipAddress = firstNonBlank(
                valueOrNull(requestContext != null ? requestContext.getIpAddress() : null),
                valueOrNull(session.getIpAddress()),
                valueOrNull(device != null ? device.getLastIp() : null)
        );

        String country = firstNonBlank(
                valueOrNull(requestContext != null ? requestContext.getLocationCountry() : null),
                valueOrNull(device != null ? device.getLastCountry() : null)
        );
        String city = firstNonBlank(
                valueOrNull(requestContext != null ? requestContext.getLocationCity() : null),
                valueOrNull(device != null ? device.getLastCity() : null)
        );

        if ((country == null || country.isBlank()) && ipAddress != null && !ipAddress.isBlank()) {
            GeoIpService.GeoLocation geoLocation = geoIpService.lookup(ipAddress);
            if (geoLocation != null) {
                country = firstNonBlank(country, valueOrNull(geoLocation.getCountryCode()), valueOrNull(geoLocation.getCountry()));
                city = firstNonBlank(city, valueOrNull(geoLocation.getCity()));
            }
        }

        String location = formatLocation(city, country);
        String userName = resolveUserName(session.getIamUser(), identifier);
        String deviceType = firstNonBlank(
                valueOrNull(device != null ? device.getDeviceType() : null),
                valueOrNull(device != null ? device.getPlatform() : null),
                UNKNOWN
        );
        String browser = formatBrowser(device);
        String channel = session.getChannel() != null ? session.getChannel().name() : UNKNOWN;
        String loginTime = OffsetDateTime.now().toString();

        return new AlertContext(
                userName,
                deviceType,
                browser,
                location,
                firstNonBlank(ipAddress, UNKNOWN),
                channel,
                loginTime
        );
    }

    private Optional<DeviceEntity> resolveDevice(String hashedDeviceId) {
        if (hashedDeviceId == null || hashedDeviceId.isBlank()) {
            return Optional.empty();
        }
        return deviceRepository.findByDeviceIdAndActiveTrue(hashedDeviceId);
    }

    private boolean isAlertAlreadySent(SessionEntity session) {
        return session.getMetadata() != null && session.getMetadata().containsKey(ALERT_SENT_AT);
    }

    private void markAlertSent(SessionEntity session, String mode) {
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        session.getMetadata().put(ALERT_SENT_AT, OffsetDateTime.now().toString());
        session.getMetadata().put(ALERT_SENT_MODE, mode);
        loginFlowService.save(session);
    }

    private void markAlertSentSafely(SessionEntity session, String mode) {
        try {
            markAlertSent(session, mode);
        } catch (Exception e) {
            log.warn("Failed to persist login alert status for sessionId={}: {}", session.getSessionId(), e.getMessage());
        }
    }

    private String resolveUserName(IamUserEntity user, String identifier) {
        String fallback = valueOrNull(identifier);
        return fallback != null ? fallback : "Customer";
    }

    private String formatBrowser(DeviceEntity device) {
        if (device == null) {
            return UNKNOWN;
        }
        String browserName = valueOrNull(device.getBrowser());
        String browserVersion = valueOrNull(device.getBrowserVersion());
        if (browserName == null && browserVersion == null) {
            return UNKNOWN;
        }
        if (browserName == null) {
            return browserVersion;
        }
        if (browserVersion == null) {
            return browserName;
        }
        return browserName + " " + browserVersion;
    }

    private String formatLocation(String city, String country) {
        if (city != null && country != null) {
            return city + ", " + country;
        }
        if (city != null) {
            return city;
        }
        if (country != null) {
            return country;
        }
        return UNKNOWN;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String cleaned = valueOrNull(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    private String valueOrNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private record AlertContext(
            String userName,
            String deviceType,
            String browser,
            String location,
            String ipAddress,
            String channel,
            String loginTime
    ) {}
}
