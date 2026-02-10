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
     * Send login alert notification
     *
     * @param channel   Notification channel
     * @param recipient Phone number or email
     * @param userName  User's name
     * @param ipAddress Login IP address
     * @param location  Login location
     * @param device    Device information
     * @return Notification response
     */
    public SendNotificationResponse sendLoginAlert(
            ChannelType channel,
            String recipient,
            String userName,
            String ipAddress,
            String location,
            String device) {

        log.info("Sending login alert via {} to: {}", channel, recipient);

        SendNotificationRequest request = SendNotificationRequest.builder()
                .channel(channel)
                .recipient(recipient)
                .templateCode("login_alert")
                .language("en")
                .parameters(Map.of(
                        "param1", userName,
                        "param2", ipAddress,
                        "param3", location,
                        "param4", device
                ))
                .metadata(Map.of(
                        "source", "iam-service",
                        "priority", "HIGH"
                ))
                .build();

        // Use async for alerts
        return notificationClient.sendAsync(request);
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
