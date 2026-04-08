package ke.shiva.sbs_iam.modules.iam.app.service;


import ke.shiva.sbs_iam.modules.iam.api.request.PasswordLoginRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.PasswordStepResponse;
import ke.shiva.sbs_iam.modules.iam.app.exception.PasswordVerificationException;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.UserContact;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.ContactType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.external.NotificationService;
import ke.shiva.sbs_iam.modules.iam.infra.repository.UserContactRepository;
import ke.shiva.shivacorestarter.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordAuthService {

    private final LoginFlowService loginFlowService;
    private final PasswordVerifier passwordVerifier;
    private final SecurityEventService securityEventService; // records LOGIN_FAILURE / SUCCESS
    private final LoginHistoryService loginHistoryService;
    private final IamUserService iamUserService;
    private final PolicyService policyService;
    private final UserContactRepository userContactRepository;
    private final NotificationService notificationService;

    @Transactional
    public PasswordStepResponse handle(PasswordLoginRequest req, UUID flowId) {

        // 1. Load session & ensure correct stage (IDENTIFIER_OK)
        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.IDENTIFIER_OK);
        IamUserEntity user = session.getIamUser();

        // Extract identifier from session metadata or use a default value
        String identifier = loginFlowService.extractIdentifier(session);

        // 2. Verify password against correct credentials table
        PasswordVerifier.PasswordVerificationResult verification = passwordVerifier.verifyWithDetails(session, req.getPassword());

        if (!verification.authenticated()) {
            securityEventService.onLoginFailure(user, "PASSWORD_INVALID", session);
            loginHistoryService.logPasswordFailure(user, identifier, session, "PASSWORD_INVALID");
            PasswordVerifier.PasswordFailureDetails failureDetails = verification.failureDetails();
            sendInternetFailedLoginAlerts(session, user, identifier, failureDetails);
            String message = buildInvalidPasswordMessage(failureDetails);
            Map<String, Object> data = buildFailedAttemptData(failureDetails);
            throw PasswordVerificationException.invalidCredentials(message, data);
        }

        securityEventService.onLoginSuccess(user, "PASSWORD_SUCCESS", session);
        loginHistoryService.logPasswordSuccess(user, identifier, session);

        // 3. Read requirements from metadata
        LoginRequirements reqs = loginFlowService.getRequirements(session);

        // 4. Advance stage & extend session
        loginFlowService.updateStage(session, LoginStage.PASSWORD_OK);
        loginFlowService.extend(session);

        Map<String, String> contactInfo = iamUserService.getUserPrimaryContactInfo(user);
        String email = MaskingUtil.maskEmail(contactInfo.get("email"));
        String phone = MaskingUtil.maskPhone(contactInfo.get("phone"));

        PasswordStepResponse resp = new PasswordStepResponse();
        resp.setOtpRequired(reqs.isOtpRequired());
        resp.setTotpRequired(reqs.isTotpRequired());
        resp.setOtpLength(reqs.getOtpLength());
        resp.setPasswordChangeRequired(reqs.isPasswordChangeRequired());
        resp.setSecurityQuestionsRequired(reqs.isQuestionsRequired());
        resp.setProfileSelectionRequired(reqs.isProfileSelectionRequired());
        resp.setEmail(email);
        resp.setPhoneNumber(phone);
        resp.setAllowedNotificationChannels(policyService.getAllowedNotificationChannels(session.getChannel()));

        return resp;
    }

    private String buildInvalidPasswordMessage(PasswordVerifier.PasswordFailureDetails failureDetails) {
        if (failureDetails == null) {
            return "Invalid credentials";
        }

        if (failureDetails.accountLocked()) {
            return "Invalid credentials. Your account has been temporarily locked after too many failed attempts. Please try again later or contact support.";
        }

        int remainingAttempts = Math.max(0, failureDetails.remainingAttempts());
        return "Invalid credentials. You have " + remainingAttempts + " password attempt"
                + (remainingAttempts == 1 ? "" : "s")
                + " remaining.";
    }

    private Map<String, Object> buildFailedAttemptData(PasswordVerifier.PasswordFailureDetails failureDetails) {
        Map<String, Object> data = new HashMap<>();
        data.put("errorType", "PASSWORD_INVALID");

        if (failureDetails != null) {
            data.put("failedAttempts", failureDetails.failedAttempts());
            data.put("maxFailedAttempts", failureDetails.maxFailedAttempts());
            data.put("remainingAttempts", failureDetails.remainingAttempts());
            data.put("accountLocked", failureDetails.accountLocked());
            if (failureDetails.lockoutUntil() != null && !failureDetails.lockoutUntil().isBlank()) {
                data.put("lockoutUntil", failureDetails.lockoutUntil());
            }
        }

        return data;
    }

    private void sendInternetFailedLoginAlerts(
            SessionEntity session,
            IamUserEntity user,
            String identifier,
            PasswordVerifier.PasswordFailureDetails failureDetails
    ) {
        if (session == null || user == null || session.getChannel() != Channel.INTERNET_BANKING) {
            return;
        }

        String email = userContactRepository
                .findByIamUserAndContactTypeAndPrimaryIsTrue(user, ContactType.EMAIL)
                .map(UserContact::getContactValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(null);

        if (email == null) {
            log.debug("Skipping failed-login email alert for userId={} because primary email is missing", user.getId());
            return;
        }

        String userName = identifier == null || identifier.isBlank() ? "Customer" : identifier.trim();
        String ipAddress = session.getIpAddress() == null || session.getIpAddress().isBlank()
                ? "Unknown"
                : session.getIpAddress().trim();
        String attemptedAt = OffsetDateTime.now().toString();

        int failedAttempts = failureDetails == null ? 0 : Math.max(0, failureDetails.failedAttempts());
        int maxFailedAttempts = failureDetails == null ? 0 : Math.max(0, failureDetails.maxFailedAttempts());
        int remainingAttempts = failureDetails == null ? 0 : Math.max(0, failureDetails.remainingAttempts());
        String lockoutUntil = failureDetails == null ? null : failureDetails.lockoutUntil();

        try {
            notificationService.sendInternetFailedLoginEmailWithFallback(
                    email,
                    userName,
                    failedAttempts,
                    maxFailedAttempts,
                    remainingAttempts,
                    ipAddress,
                    attemptedAt
            );

            if (failureDetails != null && failureDetails.accountLocked()) {
                notificationService.sendInternetAccountLockedEmailWithFallback(
                        email,
                        userName,
                        failedAttempts,
                        maxFailedAttempts,
                        lockoutUntil,
                        ipAddress,
                        attemptedAt
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to send failed-login notification for userId={} (non-blocking): {}", user.getId(), ex.getMessage());
        }
    }
}
