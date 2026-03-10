package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSummary;
import ke.shiva.sbs_iam.modules.iam.app.util.RequestContextExtractor;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.CustomerProfileEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ProfileType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginFlowService {

    private final SessionRepository sessionRepo;
    private final SessionEventRepository eventRepo;
    private final CustomerProfileRepository customerRepo;
    private final OrganizationUserRepository orgRepo;
    private final SessionRepository sessionRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionUtil encryptionUtil;
    private final RequestContextExtractor requestContextExtractor;

    // -------- CREATE LOGIN FLOW --------
    public SessionEntity start(IamUserEntity user, Channel channel, LoginRequirements reqs, String identifier, String deviceId) {

        SessionEntity s = new SessionEntity();
        s.setSessionId(String.valueOf(UUID.randomUUID()));
        s.setIamUser(user);
        s.setDeviceId(HashUtil.sha256(deviceId));
        s.setChannel(channel);
        s.setStatus(LoginStage.IDENTIFIER_OK);
        s.setSessionType(SessionType.LOGIN_TEMP);
        s.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(15)));

        // store requirements as JSON in existing "metadata" column if you have it
        if (s.getMetadata() == null) s.setMetadata(new HashMap<>());
        s.getMetadata().put("requirements", reqs);
        s.getMetadata().put("identifier", identifier);

        sessionRepo.save(s);
        logEvent(s, "LOGIN_FLOW_STARTED");

        return s;
    }

    // -------- GET AND VALIDATE STAGE --------
    public SessionEntity requireStage(UUID flowId, LoginStage stage) {
        SessionEntity s = sessionRepo.findBySessionId(String.valueOf(flowId));

        if (s == null)
            throw BaseException.invalidFlow();

        if (s.getExpiresAt().isBefore(OffsetDateTime.now()))
            throw BaseException.sessionExpired("Page expired. Please refresh and try again.");

        if (s.getStatus() != stage)
            throw BaseException.invalidStage();

        return s;
    }

    public SessionEntity requireAtLeast(UUID flowId, LoginStage minStage) {
        SessionEntity s = sessionRepo.findBySessionId(String.valueOf(flowId));

        if (s == null)
            throw BaseException.invalidFlow();

        if (s.getStatus().ordinal() < minStage.ordinal())
            throw BaseException.invalidStage();

        return s;
    }

    // -------- UPDATE STAGE --------
    public void updateStage(SessionEntity s, LoginStage stage) {
        s.setStatus(stage);
        sessionRepo.save(s);
        logEvent(s, "STAGE_CHANGED_" + stage.name());
    }

    // -------- LIST PROFILES FOR IB --------
    @Transactional(readOnly = true)
    public List<ProfileSummary> getProfiles(IamUserEntity iamUser) {

        List<ProfileSummary> list = new ArrayList<>();

        //only gets profiles for verified Customers, Org users with no bank acount are unverified
        customerRepo.findByIamUserAndIsVerifiedTrue(iamUser).ifPresent(cp -> {
            list.add(new ProfileSummary("CUSTOMER", encryptionUtil.encrypt(cp.getId().toString()), cp.getFullName()));
        });

        List<OrganizationUserEntity> orgUsers = orgRepo.findAllByIamUser(iamUser);
        for (var ou : orgUsers) {
            if (isOrganizationLocked(ou)) {
                continue;
            }
            list.add(new ProfileSummary("ORG_USER", encryptionUtil.encrypt(ou.getId().toString()), ou.getOrgDisplayName()));
        }

        return list;
    }

    // -------- SELECT PROFILE --------
    public void selectProfile(SessionEntity s, ProfileType type, @NonNull Long profileId) {

        //check that  user is related to the selected profile
        if (!ownsProfile(s.getIamUser(), type, profileId)) {
            throw BaseException.badRequest("You do not have access to this profile.");
        }

        s.setSessionType(SessionType.LOGIN_ACTIVE);
        s.setProfileType(type);
        s.setProfileId(profileId);


        sessionRepo.save(s);
        logEvent(s, "PROFILE_SELECTED_" + type);
    }

    public boolean ownsProfile(IamUserEntity iamUser, ProfileType type, Long profileId) {
        return switch (type) {
            case CUSTOMER:
                CustomerProfileEntity customerProfile = customerRepo.findById(profileId).orElseThrow(BaseException::badRequest);
                yield customerProfile.getIamUser().getId().equals(iamUser.getId());
            case ORG_USER:
                OrganizationUserEntity orgUser = orgRepo.findById(profileId).orElseThrow(BaseException::badRequest);
                yield orgUser.getIamUser().getId().equals(iamUser.getId()) && !isOrganizationLocked(orgUser);
        };
    }

    public boolean hasProfile(IamUserEntity iamUser) {
        return customerRepo.findByIamUserAndIsVerifiedTrue(iamUser).isPresent() ||
                orgRepo.findAllByIamUser(iamUser).stream().anyMatch(ou -> !isOrganizationLocked(ou));
    }

    private boolean isOrganizationLocked(OrganizationUserEntity organizationUser) {
        return organizationUser != null
                && organizationUser.getOrganizationParty() != null
                && organizationUser.getOrganizationParty().getOrganization() != null
                && Boolean.TRUE.equals(organizationUser.getOrganizationParty().getOrganization().getAccountLocked());
    }

    // -------- EVENT LOGGER --------
    private void logEvent(SessionEntity s, String action) {
        SessionEventEntity event = new SessionEventEntity();
        event.setSession(s);
        event.setEventType(action);
        event.setEventAt(OffsetDateTime.now());

        RequestContextExtractor.RequestContext context = requestContextExtractor.extractContext();
        if (context != null) {
            event.setIpAddress(context.getIpAddress());
            if (context.getDeviceId() != null && !context.getDeviceId().isBlank()) {
                event.setDeviceId(HashUtil.sha256(context.getDeviceId()));
            }
            Map<String, Object> metadata = new HashMap<>();
            if (context.getLocationCountry() != null) {
                metadata.put("country", context.getLocationCountry());
            }
            if (context.getLocationCity() != null) {
                metadata.put("city", context.getLocationCity());
            }
            if (!metadata.isEmpty()) {
                event.setMetadata(metadata);
            }
        }
        eventRepo.save(event);
    }

    // ----------------------------
    // Fetch requirements from metadata
    // ----------------------------
    public LoginRequirements getRequirements(SessionEntity s) {
        Object obj = s.getMetadata().get("requirements");
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            LoginRequirements reqs = new LoginRequirements();
            reqs.setFirstLogin(Boolean.TRUE.equals(map.get("firstLogin")));
            reqs.setTotpRequired(Boolean.TRUE.equals(map.get("totpRequired")));
            reqs.setOtpRequired(Boolean.TRUE.equals(map.get("otpRequired")));
            Object otpLength = map.get("otpLength");
            if (otpLength instanceof Number number) {
                reqs.setOtpLength(number.shortValue());
            }
            reqs.setPasswordExpired(Boolean.TRUE.equals(map.get("passwordExpired")));
            reqs.setQuestionsRequired(Boolean.TRUE.equals(map.get("questionsRequired")));
            reqs.setProfileSelectionRequired(Boolean.TRUE.equals(map.get("profileSelectionRequired")));
            return reqs;
        } else {
            return (LoginRequirements) obj;
        }
    }

    /**
     * Extract identifier from session metadata if available
     */
    public String extractIdentifier(SessionEntity session) {
        if (session.getMetadata() != null && session.getMetadata().containsKey("identifier")) {
            return (String) session.getMetadata().get("identifier");
        }
        // Fallback to user's primary identifier or username
        return session.getIamUser() != null ? String.valueOf(session.getIamUser().getId()) : "unknown";
    }

    public void save(SessionEntity s) {
        sessionRepository.save(s);
    }

    public void extend(SessionEntity s) {
        s.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(15)));
        sessionRepo.save(s);
        logEvent(s, "SESSION_EXTENDED");
    }

    public void extend(SessionEntity s, int timeMinutes) {
        s.setExpiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(timeMinutes)));
        sessionRepo.save(s);
        logEvent(s, "SESSION_EXTENDED");
    }

    public void verifyDeviceId(String deviceId) {
        String hashedDeviceId = HashUtil.sha256(deviceId);
        boolean exists = deviceRepository.existsByDeviceId(hashedDeviceId);
        if (!exists) {
            log.warn("Device ID verification failed for deviceId: {}", deviceId);
            throw BaseException.badRequest("Invalid Request");
        }
    }
}
