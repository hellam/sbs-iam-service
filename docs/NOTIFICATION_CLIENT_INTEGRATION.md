# Notification Client Integration - IAM Service

## ✅ Integration Complete

The notification client has been successfully integrated into the IAM service, enabling it to send OTP, welcome messages, alerts, and other notifications via SMS, Email, and WhatsApp.

---

## 📦 Changes Made

### 1. **pom.xml** ✅
Added notification client dependency:

```xml
<!-- Notification Client for sending notifications -->
<dependency>
    <groupId>ke.shiva</groupId>
    <artifactId>notification-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. **application.yaml** ✅
Added notification client configuration:

```yaml
shiva:
  notification:
    url: ${NOTIFICATION_SERVICE_URL:http://localhost:9003}
    kafka-topic: ${NOTIFICATION_KAFKA_TOPIC:notifications.bulk.v1}
    timeout-seconds: ${NOTIFICATION_TIMEOUT:10}

logging:
  level:
    ke.shiva.client.notification: DEBUG
```

### 3. **NotificationService.java** ✅
Created service with ready-to-use notification methods:

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationClientV1 notificationClient;

    // Methods:
    // - sendOtpViaSms()
    // - sendOtpViaWhatsApp()
    // - sendOtpViaEmail()
    // - sendWelcomeMessage()
    // - sendPasswordResetLink()
    // - sendLoginAlert()
    // - sendAccountVerificationLink()
}
```

### 4. **NOTIFICATION_INTEGRATION.env** ✅
Created configuration guide with examples

---

## 🎯 Usage Examples

### Example 1: Send OTP via SMS

```java
@Autowired
private NotificationService notificationService;

public void sendOtp(String phoneNumber) {
    String otp = generateOtp(); // Your OTP generation logic
    
    try {
        SendNotificationResponse response = 
            notificationService.sendOtpViaSms(phoneNumber, otp);
        
        if ("SENT".equals(response.getStatus())) {
            log.info("OTP sent successfully: {}", response.getNotificationId());
        } else {
            log.error("Failed to send OTP: {}", response.getMessage());
        }
    } catch (NotificationClientException e) {
        log.error("Notification service error", e);
        // Handle error appropriately
    }
}
```

### Example 2: Send OTP via WhatsApp with Copy Button

```java
public void sendOtpViaWhatsApp(String phoneNumber) {
    String otp = generateOtp();
    
    SendNotificationResponse response = 
        notificationService.sendOtpViaWhatsApp(phoneNumber, otp);
    
    // WhatsApp will show a button to copy the OTP
}
```

### Example 3: Send Welcome Message

```java
public void onUserRegistration(User user) {
    notificationService.sendWelcomeMessage(
        ChannelType.SMS,
        user.getPhoneNumber(),
        user.getFullName(),
        Map.of("accountNumber", user.getAccountNumber())
    );
}
```

### Example 4: Send Password Reset

```java
public void initiatePasswordReset(String email, String userName) {
    String resetToken = generateResetToken();
    String resetLink = buildResetLink(resetToken);
    
    notificationService.sendPasswordResetLink(
        email,
        userName,
        resetLink
    );
}
```

### Example 5: Send Login Alert

```java
public void onSuccessfulLogin(User user, HttpServletRequest request) {
    String ipAddress = getClientIp(request);
    String location = getLocationFromIp(ipAddress);
    String device = getUserAgent(request);
    
    notificationService.sendLoginAlert(
        ChannelType.EMAIL,
        user.getEmail(),
        user.getFullName(),
        ipAddress,
        location,
        device
    );
}
```

---

## 🔧 Environment Configuration

### Required Environment Variables

```bash
# Notification Service URL
NOTIFICATION_SERVICE_URL=http://localhost:9003

# Kafka Topic for async notifications
NOTIFICATION_KAFKA_TOPIC=notifications.bulk.v1

# Request timeout
NOTIFICATION_TIMEOUT=10
```

### Add to .env File

```bash
# Copy to your .env file
NOTIFICATION_SERVICE_URL=http://localhost:9003
NOTIFICATION_KAFKA_TOPIC=notifications.bulk.v1
NOTIFICATION_TIMEOUT=10
```

---

## 📋 Available Notification Methods

### OTP Methods

| Method | Channel | Mode | Use Case |
|--------|---------|------|----------|
| `sendOtpViaSms()` | SMS | SYNC | Login, verification |
| `sendOtpViaWhatsApp()` | WhatsApp | SYNC | Login with button |
| `sendOtpViaEmail()` | Email | SYNC | Email verification |

### User Account Methods

| Method | Channel | Mode | Use Case |
|--------|---------|------|----------|
| `sendWelcomeMessage()` | Any | ASYNC | New user registration |
| `sendPasswordResetLink()` | Email | SYNC | Password recovery |
| `sendAccountVerificationLink()` | Email | SYNC | Email verification |

### Security Methods

| Method | Channel | Mode | Use Case |
|--------|---------|------|----------|
| `sendLoginAlert()` | Any | ASYNC | Suspicious login |

---

## 🎨 Notification Channels

### SMS
- **Provider:** Hormuud or configured SMS provider
- **Format:** E.164 phone numbers (`+252770612971`)
- **Best for:** OTP, quick alerts

### Email
- **Provider:** Configured email server
- **Format:** Valid email addresses
- **Best for:** Detailed notifications, links

### WhatsApp
- **Provider:** WhatsApp Cloud API
- **Format:** E.164 phone numbers
- **Features:** Templates, buttons, rich media
- **Best for:** OTP with copy button, engagement

---

## 🔄 Notification Modes

### SYNC Mode (Immediate)
```java
notificationClient.sendSync(request);
```
- Waits for delivery result
- Use for critical notifications (OTP, security)
- Returns actual delivery status

### ASYNC Mode (Queued)
```java
notificationClient.sendAsync(request);
```
- Returns immediately
- Queued to Kafka for processing
- Use for non-critical notifications (welcome, alerts)

**Note:** CRITICAL priority automatically uses SYNC mode even with sendAsync()

---

## 🧪 Testing

### 1. Start Notification Service

```bash
cd services/sbs-notification-service-v1
mvn spring-boot:run
```

### 2. Start IAM Service

```bash
cd services/sbs-iam-service
mvn spring-boot:run
```

### 3. Test Notification

```java
@RestController
@RequestMapping("/api/v1/oauth/test")
public class NotificationTestController {
    
    @Autowired
    private NotificationService notificationService;
    
    @PostMapping("/send-otp-sms")
    public ResponseEntity<?> testOtpSms(@RequestBody TestOtpRequest request) {
        SendNotificationResponse response = 
            notificationService.sendOtpViaSms(
                request.getPhoneNumber(), 
                request.getOtp()
            );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/send-otp-whatsapp")
    public ResponseEntity<?> testOtpWhatsApp(@RequestBody TestOtpRequest request) {
        SendNotificationResponse response = 
            notificationService.sendOtpViaWhatsApp(
                request.getPhoneNumber(), 
                request.getOtp()
            );
        return ResponseEntity.ok(response);
    }
}
```

---

## 🚨 Error Handling

### Best Practices

```java
try {
    SendNotificationResponse response = 
        notificationService.sendOtpViaSms(phoneNumber, otp);
    
    if ("SENT".equals(response.getStatus())) {
        // Success - OTP delivered
        log.info("OTP sent: {}", response.getNotificationId());
    } else if ("QUEUED".equals(response.getStatus())) {
        // Async - queued for processing
        log.info("OTP queued: {}", response.getNotificationId());
    } else {
        // Failed
        log.error("OTP failed: {}", response.getMessage());
        throw new OtpDeliveryException(response.getMessage());
    }
    
} catch (NotificationClientException e) {
    log.error("Notification service unavailable", e);
    // Implement fallback or retry logic
    throw new ServiceUnavailableException("Cannot send OTP at this time");
}
```

### Retry Strategy

```java
@Retryable(
    value = {NotificationClientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000)
)
public void sendOtpWithRetry(String phoneNumber, String otp) {
    notificationService.sendOtpViaSms(phoneNumber, otp);
}
```

---

## 📊 Integration Flow

### OTP Login Flow

```
1. User enters phone number
   ↓
2. IAM Service generates OTP
   ↓
3. NotificationService.sendOtpViaSms()
   ↓
4. Notification Client → Notification Service
   ↓
5. Notification Service → SMS Provider (Hormuud)
   ↓
6. User receives OTP
   ↓
7. User enters OTP
   ↓
8. IAM Service verifies and issues token
```

### Welcome Message Flow

```
1. User registration complete
   ↓
2. NotificationService.sendWelcomeMessage()
   ↓
3. Notification Client → Kafka
   ↓
4. Kafka → Notification Service Consumer
   ↓
5. Notification Service → Channel (SMS/Email/WhatsApp)
   ↓
6. User receives welcome message
```

---

## ✅ Compilation Status

```
[INFO] Building iam-service 1.0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 🎯 Next Steps

### 1. Configure Environment

Add to your `.env` file:
```bash
NOTIFICATION_SERVICE_URL=http://localhost:9003
NOTIFICATION_KAFKA_TOPIC=notifications.bulk.v1
NOTIFICATION_TIMEOUT=10
```

### 2. Integrate in Your Code

```java
@Autowired
private NotificationService notificationService;

// Use in your authentication flow, registration, etc.
```

### 3. Create Templates

Ensure templates exist in Notification Service:
- `otp_verification` - For OTP messages
- `welcome_message` - For new users
- `password_reset` - For password recovery
- `login_alert` - For security alerts
- `account_verification` - For email verification

### 4. Test End-to-End

1. Start Notification Service
2. Start IAM Service
3. Trigger notification (login, registration, etc.)
4. Verify delivery

---

## 📚 Documentation

- **Configuration:** `NOTIFICATION_INTEGRATION.env`
- **Usage Examples:** See this guide
- **Notification Client Docs:** `shiva-service-clients/notification-client/`
- **Notification Service Docs:** `services/sbs-notification-service-v1/`

---

## 🎉 Summary

**✅ Integration Complete**

- ✅ Notification client dependency added
- ✅ Configuration added to application.yaml
- ✅ NotificationService created with 7 ready-to-use methods
- ✅ Compilation successful
- ✅ Examples and documentation provided

**Ready to Use:**
- Send OTP via SMS, Email, or WhatsApp
- Send welcome messages
- Send password reset links
- Send login alerts
- Send account verification links

**Channels Supported:**
- ✅ SMS (via Hormuud)
- ✅ Email
- ✅ WhatsApp (with button support)

---

**Date:** February 9, 2026  
**Status:** ✅ Complete and Ready  
**IAM Service Version:** 1.0.1-SNAPSHOT  
**Notification Client Version:** 1.0.0-SNAPSHOT

