package ke.shiva.sbs_iam.modules.iam.infra.external;

import ke.shiva.client.notification.v1.NotificationClientV1;
import ke.shiva.client.notification.v1.enums.ChannelType;
import ke.shiva.client.notification.v1.dto.SendNotificationRequest;
import ke.shiva.client.notification.v1.dto.SendNotificationResponse;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
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
        log.info("Sending OTP via Email to: {}", email);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(email)
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
     * Send welcome message via preferred channel
     *
     * @param channel         The notification channel (SMS, EMAIL, or WHATSAPP)
     * @param recipient       Phone number or email
     * @param userName        User's name
     * @param additionalInfo  Additional information map
     * @return Notification response
     */
    public SendNotificationResponse sendWelcomeMessage(
            ChannelType channel,
            String recipient,
            String userName,
            Map<String, Object> additionalInfo) {

        log.info("Sending welcome message via {} to: {}", channel, recipient);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", userName);
        if (additionalInfo != null) {
            parameters.putAll(additionalInfo);
        }

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(channel)
                .recipient(recipient)
                .templateCode("welcome_message")
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

        log.info("Sending password reset link to: {}", email);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(email)
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

        return notificationClient.sendSync(request);
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
        log.info("Sending admin password reset notice to: {}", email);
        String safeReference = reference == null || reference.isBlank() ? "-" : reference;
        String safePassword = temporaryPassword == null ? "" : temporaryPassword.trim();
        String message = "Hello " + (userName == null || userName.isBlank() ? "Customer" : userName)
                + ", your login password was reset by support. "
                + "Temporary password: " + safePassword + ". "
                + "Please log in and change it immediately. Ref: " + safeReference;
        return notificationClient.sendDirectMessage(ChannelType.EMAIL, email, message);
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
        log.info("Sending organization profile linked notice to: {}", email);
        String safeName = userName == null || userName.isBlank() ? "Customer" : userName;
        String safeOrganizationName = organizationName == null || organizationName.isBlank() ? "your company" : organizationName;
        String safeOrganizationId = organizationClientId == null || organizationClientId.isBlank() ? "-" : organizationClientId;
        String message = "Hello " + safeName
                + ", you have been added to company profile " + safeOrganizationName + " (" + safeOrganizationId + "). "
                + "Use your existing internet banking credentials to log in, then select this company profile to view and perform actions.";
        return notificationClient.sendDirectMessage(ChannelType.EMAIL, email, message);
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
        log.info("Sending organization profile onboarded notice to: {}", email);
        String safeName = userName == null || userName.isBlank() ? "Customer" : userName;
        String safeOrganizationName = organizationName == null || organizationName.isBlank() ? "your company" : organizationName;
        String safeOrganizationId = organizationClientId == null || organizationClientId.isBlank() ? "-" : organizationClientId;
        String safeUsername = username == null ? "" : username.trim();
        String safePassword = temporaryPassword == null ? "" : temporaryPassword.trim();
        String message = "Hello " + safeName
                + ", you have been onboarded and added to company profile " + safeOrganizationName + " (" + safeOrganizationId + "). "
                + "Username: " + safeUsername + ". Temporary password: " + safePassword + ". "
                + "Log in, select this company profile, and change your password immediately.";
        return notificationClient.sendDirectMessage(ChannelType.EMAIL, email, message);
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
        log.info("Sending login alert via EMAIL to: {}", email);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(email)
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
        log.info("Sending direct login alert via EMAIL to: {}", email);

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
                .recipient(email)
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

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
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

        log.info("Sending account verification link to: {}", email);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(ChannelType.EMAIL)
                .recipient(email)
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
