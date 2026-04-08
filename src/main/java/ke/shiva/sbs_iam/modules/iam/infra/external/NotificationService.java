package ke.shiva.sbs_iam.modules.iam.infra.external;

import ke.shiva.client.notification.v1.NotificationClientV1;
import ke.shiva.client.notification.v1.enums.ChannelType;
import ke.shiva.client.notification.v1.dto.SendNotificationRequest;
import ke.shiva.client.notification.v1.dto.SendNotificationResponse;
import ke.shiva.client.notification.v1.exception.TemplateNotConfiguredException;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service for sending OTP and other notifications
 * Uses the notification client to send via SMS, Email, or WhatsApp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationClientV1 notificationClient;
    private final Environment environment;

    @Value("${shiva.notification.dev-email-recipient:${shiva.notifications.dev-email-recipient:}}")
    private String devEmailRecipient;

    @Value("${shiva.notification.internet-banking-login-url:${shiva.notifications.internet-banking-login-url:https://banking.shiva.ke}}")
    private String internetBankingLoginUrl;

    public SendNotificationResponse sendOtp(NotificationChannel channel,
                                            String recipient, String otp, Integer otpExpirySeconds){
        String expiryMinutes = String.valueOf(otpExpirySeconds/60);
        switch (channel){
            case NotificationChannel.EMAIL -> {
                return sendOtpViaEmail(recipient, otp,expiryMinutes);
            }
            case NotificationChannel.SMS -> {
                return sendOtpViaSms(recipient, otp,expiryMinutes);
            }
            case NotificationChannel.WHATSAPP -> {
                return sendOtpViaWhatsApp(recipient, otp,expiryMinutes);
            }
            default -> throw BaseException.badRequest("Invalid Request");
        }
    }

    /**
     * Send OTP via SMS
     *
     * @param phoneNumber   Recipient phone number in E.164 format
     * @param otp           The OTP code
     * @param expiryMinutes
     * @return Notification response
     */
    public SendNotificationResponse sendOtpViaSms(String phoneNumber, String otp, String expiryMinutes) {
        log.info("Sending OTP via SMS to: {}", phoneNumber);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.SMS)
                .recipient(phoneNumber)
                .templateCode("otp_verification")
                .language("en")
                .parameters(Map.of(
                        "otp", otp,
                        "validity", expiryMinutes
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "CRITICAL"
                ))
                .build();

        return notificationClient.sendSync(request);
    }

    /**
     * Send OTP via WhatsApp with copy button
     *
     * @param phoneNumber   Recipient phone number in E.164 format
     * @param otp           The OTP code
     * @param expiryMinutes
     * @return Notification response
     */
    public SendNotificationResponse sendOtpViaWhatsApp(String phoneNumber, String otp, String expiryMinutes) {
        log.info("Sending OTP via WhatsApp to: {}", phoneNumber);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.WHATSAPP)
                .recipient(phoneNumber)
                .templateCode("otp_verification")
                .language("en")
                .parameters(Map.of(
                        "param1", otp,
                        "param2", expiryMinutes
                ))
                .buttonParameters(List.of(
                        List.of(otp) // OTP for the copy button
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "CRITICAL"
                ))
                .build();

        return notificationClient.sendSync(request);
    }

    /**
     * Send OTP via Email
     *
     * @param email         Recipient email address
     * @param otp           The OTP code
     * @param expiryMinutes
     * @return Notification response
     */
    public SendNotificationResponse sendOtpViaEmail(String email, String otp, String expiryMinutes) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending OTP via Email to: {}", recipient);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .templateCode("otp_verification")
                .language("en")
                .parameters(Map.of(
                        "otp", otp,
                        "validity", expiryMinutes
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "CRITICAL"
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    /**
     * Send welcome message via preferred channel
     *
     * @param channel         The notification channel (SMS, EMAIL, or WHATSAPP)
     * @param recipient       Phone number or email
     * @param name            User's display name
     * @param additionalInfo  Additional information map
     * @return Notification response
     */
    public SendNotificationResponse sendWelcomeMessage(
            ChannelType channel,
            String recipient,
            String name,
            Map<String, Object> additionalInfo) {
        String resolvedRecipient = resolveRecipient(channel, recipient);

        log.info("Sending welcome message via {} to: {}", channel, resolvedRecipient);

        Map<String, Object> parameters = new HashMap<>();
        if (additionalInfo != null) {
            parameters.putAll(additionalInfo);
        }

        String resolvedName = safe(name, "Customer");
        String resolvedUserName = firstNonBlank(
                readText(parameters, "userName"),
                readText(parameters, "username"),
                resolvedName
        );
        String resolvedPassword = firstNonBlank(
                readText(parameters, "password"),
                "Use your existing password"
        );
        String resolvedLoginUrl = firstNonBlank(
                readText(parameters, "loginUrl"),
                internetBankingLoginUrl
        );

        parameters.put("name", resolvedName);
        parameters.put("userName", resolvedUserName);
        parameters.put("password", resolvedPassword);
        parameters.put("loginUrl", resolvedLoginUrl);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(channel)
                .recipient(resolvedRecipient)
                .templateCode("welcome_internet_banking")
                .language("en")
                .parameters(parameters)
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "NORMAL"
                ))
                .build();

        // Use async for non-critical notifications
        return notificationClient.sendAsync(request);
    }

    /**
     * Send password reset link
     *
     * @param email      User's email
     * @param userName   User's name
     * @param resetToken Reset token/link
     * @return Notification response
     */
    public SendNotificationResponse sendPasswordResetLink(
            String email,
            String userName,
            String resetToken) {
        String recipient = resolveEmailRecipient(email);

        log.info("Sending password reset link to: {}", recipient);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .templateCode("password_reset")
                .language("en")
                .parameters(Map.of(
                        "param1", userName,
                        "param2", resetToken
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "CRITICAL"
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    /**
     * Send admin-triggered password reset notice with generated temporary password.
     */
    public SendNotificationResponse sendAdminPasswordResetNotice(
            String email,
            String userName,
            String reference,
            String temporaryPassword
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending admin password reset notice to: {}", recipient);
        String safeReference = reference == null || reference.isBlank() ? "-" : reference;
        String safePassword = temporaryPassword == null ? "" : temporaryPassword.trim();
        String message = "Hello " + (userName == null || userName.isBlank() ? "Customer" : userName)
                + ", your login password was reset by support. "
                + "Temporary password: " + safePassword + ". "
                + "Please log in and change it immediately. Ref: " + safeReference;
        return notificationClient.sendDirectMessage(ChannelType.EMAIL, recipient, message);
    }

    /**
     * Notify an existing user that they were linked to a company profile.
     */
    public SendNotificationResponse sendOrganizationProfileLinkedNotice(
            String email,
            String userName,
            String organizationName,
            String organizationClientId
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending organization profile linked notice to: {}", recipient);
        String safeName = userName == null || userName.isBlank() ? "Customer" : userName;
        String safeOrganizationName = organizationName == null || organizationName.isBlank() ? "your company" : organizationName;
        String safeOrganizationId = organizationClientId == null || organizationClientId.isBlank() ? "-" : organizationClientId;
        String message = "Hello " + safeName
                + ", you have been added to company profile " + safeOrganizationName + " (" + safeOrganizationId + "). "
                + "Use your existing internet banking credentials to log in, then select this company profile to view and perform actions.";
        return notificationClient.sendDirectMessage(ChannelType.EMAIL, recipient, message);
    }

    /**
     * Notify a newly onboarded non-bank user with generated login credentials and profile-link information.
     */
    public SendNotificationResponse sendOrganizationProfileOnboardedNotice(
            String email,
            String userName,
            String organizationName,
            String organizationClientId,
            String username,
            String temporaryPassword
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending organization profile onboarded notice to: {}", recipient);
        String safeName = userName == null || userName.isBlank() ? "Customer" : userName;
        String safeOrganizationName = organizationName == null || organizationName.isBlank() ? "your company" : organizationName;
        String safeOrganizationId = organizationClientId == null || organizationClientId.isBlank() ? "-" : organizationClientId;
        String safeUsername = username == null ? "" : username.trim();
        String safePassword = temporaryPassword == null ? "" : temporaryPassword.trim();
        String message = "Hello " + safeName
                + ", you have been onboarded and added to company profile " + safeOrganizationName + " (" + safeOrganizationId + "). "
                + "Username: " + safeUsername + ". Temporary password: " + safePassword + ". "
                + "Log in, select this company profile, and change your password immediately.";
        return notificationClient.sendDirectMessage(ChannelType.EMAIL, recipient, message);
    }

    public SendNotificationResponse sendLoginAlertEmail(
            String email,
            String userName,
            String deviceType,
            String browser,
            String location,
            String ipAddress,
            String channel,
            String loginTime
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending login alert via EMAIL to: {}", recipient);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .templateCode("login_alert")
                .eventType("login_alert")
                .language("en")
                .parameters(Map.of(
                        "userName", safe(userName, "Customer"),
                        "deviceType", safe(deviceType, "Unknown"),
                        "browser", safe(browser, "Unknown"),
                        "location", safe(location, "Unknown"),
                        "ipAddress", safe(ipAddress, "Unknown"),
                        "channel", safe(channel, "Unknown"),
                        "loginTime", safe(loginTime, "Unknown")
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH",
                        "templateType", "login-alert",
                        "customerName", safe(userName, "Customer")
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    public SendNotificationResponse sendLoginAlertDirectEmail(
            String email,
            String userName,
            String deviceType,
            String browser,
            String location,
            String ipAddress,
            String channel,
            String loginTime
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending direct login alert via EMAIL to: {}", recipient);

        String message = "Hello " + safe(userName, "Customer") + ", a new login to your account was detected.\n"
                + "Channel: " + safe(channel, "Unknown") + "\n"
                + "Device Type: " + safe(deviceType, "Unknown") + "\n"
                + "Browser: " + safe(browser, "Unknown") + "\n"
                + "Location: " + safe(location, "Unknown") + "\n"
                + "IP Address: " + safe(ipAddress, "Unknown") + "\n"
                + "Login Time: " + safe(loginTime, "Unknown") + "\n\n"
                + "If this wasn't you, please contact support immediately.";

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .message(message)
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH",
                        "subject", "New login detected on your account",
                        "templateType", "login-alert",
                        "customerName", safe(userName, "Customer")
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    public void sendInternetFailedLoginEmailWithFallback(
            String email,
            String userName,
            int failedAttempts,
            int maxFailedAttempts,
            int remainingAttempts,
            String ipAddress,
            String attemptedAt
    ) {
        try {
            sendInternetFailedLoginEmail(
                    email,
                    userName,
                    failedAttempts,
                    maxFailedAttempts,
                    remainingAttempts,
                    ipAddress,
                    attemptedAt
            );
        } catch (Exception templateError) {
            if (!shouldFallbackToDirectTemplate(templateError)) {
                throw templateError;
            }
            log.warn("Template internet_failed_login is unavailable, using direct email fallback: {}", templateError.getMessage());
            sendInternetFailedLoginDirectEmail(
                    email,
                    userName,
                    failedAttempts,
                    maxFailedAttempts,
                    remainingAttempts,
                    ipAddress,
                    attemptedAt
            );
        }
    }

    public void sendInternetAccountLockedEmailWithFallback(
            String email,
            String userName,
            int failedAttempts,
            int maxFailedAttempts,
            String lockoutUntil,
            String ipAddress,
            String attemptedAt
    ) {
        try {
            sendInternetAccountLockedEmail(
                    email,
                    userName,
                    failedAttempts,
                    maxFailedAttempts,
                    lockoutUntil,
                    ipAddress,
                    attemptedAt
            );
        } catch (Exception templateError) {
            if (!shouldFallbackToDirectTemplate(templateError)) {
                throw templateError;
            }
            log.warn("Template internet_account_locked is unavailable, using direct email fallback: {}", templateError.getMessage());
            sendInternetAccountLockedDirectEmail(
                    email,
                    userName,
                    failedAttempts,
                    maxFailedAttempts,
                    lockoutUntil,
                    ipAddress,
                    attemptedAt
            );
        }
    }

    public SendNotificationResponse sendInternetFailedLoginEmail(
            String email,
            String userName,
            int failedAttempts,
            int maxFailedAttempts,
            int remainingAttempts,
            String ipAddress,
            String attemptedAt
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending internet failed-login alert via EMAIL to: {}", recipient);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .templateCode("internet_failed_login")
                .eventType("internet_failed_login")
                .language("en")
                .parameters(Map.of(
                        "userName", safe(userName, "Customer"),
                        "failedAttempts", String.valueOf(Math.max(0, failedAttempts)),
                        "maxFailedAttempts", String.valueOf(Math.max(0, maxFailedAttempts)),
                        "remainingAttempts", String.valueOf(Math.max(0, remainingAttempts)),
                        "ipAddress", safe(ipAddress, "Unknown"),
                        "channel", "INTERNET_BANKING",
                        "attemptedAt", safe(attemptedAt, "Unknown")
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH",
                        "templateType", "notification",
                        "customerName", safe(userName, "Customer")
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    public SendNotificationResponse sendInternetFailedLoginDirectEmail(
            String email,
            String userName,
            int failedAttempts,
            int maxFailedAttempts,
            int remainingAttempts,
            String ipAddress,
            String attemptedAt
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending direct internet failed-login alert via EMAIL to: {}", recipient);

        String message = "Hello " + safe(userName, "Customer") + ", we detected a failed internet banking login attempt.\n"
                + "Failed attempts: " + Math.max(0, failedAttempts) + "/" + Math.max(0, maxFailedAttempts) + "\n"
                + "Remaining attempts: " + Math.max(0, remainingAttempts) + "\n"
                + "IP Address: " + safe(ipAddress, "Unknown") + "\n"
                + "Attempt Time: " + safe(attemptedAt, "Unknown") + "\n\n"
                + "If this was not you, please contact support immediately.";

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .message(message)
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH",
                        "subject", "Failed internet banking login attempt",
                        "templateType", "notification",
                        "customerName", safe(userName, "Customer")
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    public SendNotificationResponse sendInternetAccountLockedEmail(
            String email,
            String userName,
            int failedAttempts,
            int maxFailedAttempts,
            String lockoutUntil,
            String ipAddress,
            String attemptedAt
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending internet account-locked alert via EMAIL to: {}", recipient);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .templateCode("internet_account_locked")
                .eventType("internet_account_locked")
                .language("en")
                .parameters(Map.of(
                        "userName", safe(userName, "Customer"),
                        "failedAttempts", String.valueOf(Math.max(0, failedAttempts)),
                        "maxFailedAttempts", String.valueOf(Math.max(0, maxFailedAttempts)),
                        "lockoutUntil", safe(lockoutUntil, "Until support unlocks your account"),
                        "ipAddress", safe(ipAddress, "Unknown"),
                        "channel", "INTERNET_BANKING",
                        "attemptedAt", safe(attemptedAt, "Unknown")
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH",
                        "templateType", "notification",
                        "customerName", safe(userName, "Customer")
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    public SendNotificationResponse sendInternetAccountLockedDirectEmail(
            String email,
            String userName,
            int failedAttempts,
            int maxFailedAttempts,
            String lockoutUntil,
            String ipAddress,
            String attemptedAt
    ) {
        String recipient = resolveEmailRecipient(email);
        log.info("Sending direct internet account-locked alert via EMAIL to: {}", recipient);

        String message = "Hello " + safe(userName, "Customer") + ", your internet banking account has been locked after repeated failed login attempts.\n"
                + "Failed attempts: " + Math.max(0, failedAttempts) + "/" + Math.max(0, maxFailedAttempts) + "\n"
                + "Locked until: " + safe(lockoutUntil, "Until support unlocks your account") + "\n"
                + "IP Address: " + safe(ipAddress, "Unknown") + "\n"
                + "Attempt Time: " + safe(attemptedAt, "Unknown") + "\n\n"
                + "If this was not you, contact support immediately.";

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .message(message)
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH",
                        "subject", "Internet banking account locked",
                        "templateType", "notification",
                        "customerName", safe(userName, "Customer")
                ))
                .build();

        return notificationClient.sendAsync(request);
    }

    public String resolveDeliveryRecipient(ChannelType channel, String recipient) {
        return resolveRecipient(channel, recipient);
    }

    private String resolveRecipient(ChannelType channel, String recipient) {
        if (channel == ChannelType.EMAIL) {
            return resolveEmailRecipient(recipient);
        }
        return recipient;
    }

    private String resolveEmailRecipient(String recipient) {
        String resolved = recipient == null ? null : recipient.trim();
        if (isDevProfile() && StringUtils.hasText(devEmailRecipient)) {
            String override = devEmailRecipient.trim();
            if (StringUtils.hasText(resolved) && !override.equalsIgnoreCase(resolved)) {
                log.debug("DEV email override applied. Original recipient: {}", resolved);
            }
            return override;
        }
        return resolved;
    }

    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private boolean shouldFallbackToDirectTemplate(Exception exception) {
        if (exception instanceof TemplateNotConfiguredException) {
            return true;
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("template") || normalized.contains("no template found");
    }

    private String readText(Map<String, Object> parameters, String key) {
        if (parameters == null || key == null || key.isBlank()) {
            return null;
        }
        Object raw = parameters.get(key);
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Send account verification link
     *
     * @param email            User's email
     * @param userName         User's name
     * @param verificationLink Verification link
     * @return Notification response
     */
    public SendNotificationResponse sendAccountVerificationLink(
            String email,
            String userName,
            String verificationLink) {
        String recipient = resolveEmailRecipient(email);

        log.info("Sending account verification link to: {}", recipient);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(recipient)
                .templateCode("account_verification")
                .language("en")
                .parameters(Map.of(
                        "param1", userName,
                        "param2", verificationLink
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH"
                ))
                .build();

        return notificationClient.sendSync(request);
    }
}
