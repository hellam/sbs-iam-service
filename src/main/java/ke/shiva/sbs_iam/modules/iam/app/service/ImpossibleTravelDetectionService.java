package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.app.util.RequestContextExtractor;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.LoginHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityEventEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginHistoryRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityEventRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImpossibleTravelDetectionService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityEventRepository securityEventRepository;
    private final RequestContextExtractor requestContextExtractor;
    private final GeoIpService geoIpService;
    private final SessionRevocationService sessionRevocationService;

    @Value("${shiva.security.risk.impossible-travel.enabled:true}")
    private boolean enabled;

    @Value("${shiva.security.risk.impossible-travel.lookback-hours:24}")
    private int lookbackHours;

    @Value("${shiva.security.risk.impossible-travel.min-distance-km:500}")
    private double minDistanceKm;

    @Value("${shiva.security.risk.impossible-travel.speed-threshold-kmh:900}")
    private double speedThresholdKmh;

    @Value("${shiva.security.risk.impossible-travel.country-jump-window-minutes:45}")
    private long countryJumpWindowMinutes;

    @Value("${shiva.security.risk.user-agent-drift.enabled:true}")
    private boolean userAgentDriftEnabled;

    @Value("${shiva.security.risk.user-agent-drift.window-minutes:30}")
    private long userAgentDriftWindowMinutes;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BaseException.class)
    public void enforce(SessionEntity session) {
        if (!enabled || session == null || session.getIamUser() == null) {
            return;
        }

        RequestContextExtractor.RequestContext context = requestContextExtractor.extractContext();
        if (context == null || isBlank(context.getIpAddress())) {
            return;
        }

        Optional<LoginHistoryEntity> previousLoginOpt =
                loginHistoryRepository.findFirstByIamUserAndSuccessTrueOrderByCreatedAtDesc(session.getIamUser());
        if (previousLoginOpt.isEmpty()) {
            return;
        }

        LoginHistoryEntity previousLogin = previousLoginOpt.get();
        if (isBlank(previousLogin.getIpAddress()) || previousLogin.getCreatedAt() == null) {
            return;
        }

        if (previousLogin.getIpAddress().equals(context.getIpAddress())) {
            return;
        }

        long elapsedMinutes = ChronoUnit.MINUTES.between(previousLogin.getCreatedAt(), OffsetDateTime.now());
        if (elapsedMinutes <= 0 || elapsedMinutes > lookbackHours * 60L) {
            return;
        }

        RiskAssessment assessment = assess(previousLogin, context, elapsedMinutes);
        if (!assessment.impossibleTravel()) {
            return;
        }

        sessionRevocationService.revokeSessionAndDevice(session, "IMPOSSIBLE_TRAVEL");
        securityEventRepository.save(buildSecurityEvent(session, previousLogin, context, assessment, elapsedMinutes));

        log.error("Impossible travel detected for user={} sessionId={} reason={} elapsedMinutes={} speedKmh={}",
                session.getIamUser().getId(), session.getSessionId(), assessment.reason(),
                elapsedMinutes, assessment.speedKmh());

        throw BaseException.unauthorized("Suspicious sign-in activity detected. Please sign in again.");
    }

    private RiskAssessment assess(LoginHistoryEntity previousLogin,
                                  RequestContextExtractor.RequestContext currentContext,
                                  long elapsedMinutes) {
        GeoIpService.GeoLocation previousLocation = geoIpService.lookup(previousLogin.getIpAddress());
        GeoIpService.GeoLocation currentLocation = geoIpService.lookup(currentContext.getIpAddress());

        Double distanceKm = null;
        Double speedKmh = null;
        boolean distanceSpeedImpossible = false;

        if (hasCoordinates(previousLocation) && hasCoordinates(currentLocation)) {
            distanceKm = haversineKm(
                    previousLocation.getLatitude(),
                    previousLocation.getLongitude(),
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude()
            );
            double elapsedHours = Math.max(elapsedMinutes / 60.0, 1.0 / 60.0);
            speedKmh = distanceKm / elapsedHours;
            distanceSpeedImpossible = distanceKm >= minDistanceKm && speedKmh >= speedThresholdKmh;
        }

        String previousCountry = pickCountry(previousLogin.getLocationCountry(), previousLocation);
        String currentCountry = pickCountry(currentContext.getLocationCountry(), currentLocation);
        boolean countryJumpImpossible =
                !isBlank(previousCountry)
                        && !isBlank(currentCountry)
                        && !previousCountry.equalsIgnoreCase(currentCountry)
                        && elapsedMinutes <= countryJumpWindowMinutes;

        boolean userAgentDriftImpossible = false;
        if (userAgentDriftEnabled && elapsedMinutes <= userAgentDriftWindowMinutes) {
            boolean userAgentChanged =
                    !isBlank(previousLogin.getUserAgent())
                            && !isBlank(currentContext.getUserAgent())
                            && !previousLogin.getUserAgent().equals(currentContext.getUserAgent());

            boolean deviceChanged =
                    !isBlank(previousLogin.getDeviceId())
                            && !isBlank(currentContext.getDeviceId())
                            && !previousLogin.getDeviceId().equals(currentContext.getDeviceId());

            boolean countryChanged =
                    !isBlank(previousCountry)
                            && !isBlank(currentCountry)
                            && !previousCountry.equalsIgnoreCase(currentCountry);

            userAgentDriftImpossible = userAgentChanged && (deviceChanged || countryChanged);
        }

        String reason;
        if (distanceSpeedImpossible) {
            reason = "DISTANCE_SPEED";
        } else if (countryJumpImpossible) {
            reason = "COUNTRY_JUMP";
        } else if (userAgentDriftImpossible) {
            reason = "USER_AGENT_DRIFT";
        } else {
            reason = "NONE";
        }

        return new RiskAssessment(distanceSpeedImpossible || countryJumpImpossible || userAgentDriftImpossible, reason,
                previousCountry, currentCountry, distanceKm, speedKmh);
    }

    private SecurityEventEntity buildSecurityEvent(SessionEntity session,
                                                   LoginHistoryEntity previousLogin,
                                                   RequestContextExtractor.RequestContext currentContext,
                                                   RiskAssessment assessment,
                                                   long elapsedMinutes) {
        SecurityEventEntity event = new SecurityEventEntity();
        event.setIamUser(session.getIamUser());
        event.setRelatedSession(session);
        event.setEventType("IMPOSSIBLE_TRAVEL");
        event.setSeverity("CRITICAL");
        event.setDescription("Impossible travel detected and session revoked.");
        event.setCreatedAt(OffsetDateTime.now());
        event.setIpAddress(currentContext.getIpAddress());
        event.setDeviceId(session.getDeviceId());
        event.setLocationCountry(assessment.currentCountry());
        event.setLocationCity(currentContext.getLocationCity());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reason", assessment.reason());
        metadata.put("elapsed_minutes", elapsedMinutes);
        metadata.put("previous_ip", previousLogin.getIpAddress());
        metadata.put("current_ip", currentContext.getIpAddress());
        metadata.put("previous_country", assessment.previousCountry());
        metadata.put("current_country", assessment.currentCountry());
        metadata.put("previous_login_at", previousLogin.getCreatedAt().toString());
        if (assessment.distanceKm() != null) {
            metadata.put("distance_km", Math.round(assessment.distanceKm()));
        }
        if (assessment.speedKmh() != null) {
            metadata.put("speed_kmh", Math.round(assessment.speedKmh()));
        }
        event.setMetadata(metadata);
        return event;
    }

    private static boolean hasCoordinates(GeoIpService.GeoLocation location) {
        return location != null && location.getLatitude() != null && location.getLongitude() != null;
    }

    private static String pickCountry(String preferred, GeoIpService.GeoLocation fallback) {
        if (!isBlank(preferred)) {
            return preferred;
        }
        if (fallback != null && !isBlank(fallback.getCountryCode())) {
            return fallback.getCountryCode();
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private record RiskAssessment(
            boolean impossibleTravel,
            String reason,
            String previousCountry,
            String currentCountry,
            Double distanceKm,
            Double speedKmh
    ) {
    }
}
