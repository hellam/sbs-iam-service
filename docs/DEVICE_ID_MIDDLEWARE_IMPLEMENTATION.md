# Device ID Validation Middleware Implementation

## Overview

This implementation provides a Laravel-style middleware system for validating device IDs in Spring Boot authentication flows. It uses Spring's `HandlerInterceptor` mechanism with declarative annotations to enforce device validation rules at the controller method level.

## Architecture

### Components

1. **DeviceValidationMode** (Enum)
   - `EXISTENCE_ONLY`: Validates that device ID exists in database
   - `SESSION_BOUND`: Validates device ID exists AND matches session's device

2. **@RequiresDeviceId** (Annotation)
   - Applied to controller methods requiring device validation
   - Configurable validation mode and required flag

3. **DeviceIdInterceptor** (Middleware)
   - Intercepts requests before controller execution
   - Extracts device ID from cookie
   - Delegates validation to DeviceIdValidator

4. **DeviceIdValidator** (Service)
   - Centralized validation logic
   - Handles both EXISTENCE_ONLY and SESSION_BOUND modes
   - Provides detailed logging for security events

## Usage

### 1. Identifier Lookup (EXISTENCE_ONLY)

Use for endpoints that only need to verify a device is registered:

```java
@PostMapping("/identifier/mobile")
@RequiresDeviceId(mode = DeviceValidationMode.EXISTENCE_ONLY)
public ResponseEntity<ApiResponse<IdentifierResponse>> identifyMobile(
    @RequestBody @Valid IdentifierRequest req,
    HttpServletRequest http,
    @CookieValue(value = SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME) String deviceId
) {
    // Implementation
}
```

**Validation Flow:**
1. Extract device ID from `__Host-SBS_SID` cookie
2. Hash device ID with SHA-256
3. Check if hashed device ID exists in `devices` table with `active = true`
4. Allow request if valid, throw `BaseException.unauthorized()` if not

### 2. Authenticated Operations (SESSION_BOUND)

Use for endpoints requiring session-device binding:

```java
@PostMapping("/password")
@RequiresDeviceId(mode = DeviceValidationMode.SESSION_BOUND)
@RequiresStage(LoginStage.IDENTIFIER_OK)
public ResponseEntity<ApiResponse<PasswordStepResponse>> passwordStep(
    @RequestBody @Valid PasswordLoginRequest request, 
    @FlowId UUID flowId
) {
    // Implementation
}
```

**Validation Flow:**
1. Extract device ID from cookie and hash it
2. Validate device exists (same as EXISTENCE_ONLY)
3. Extract flow ID from request header
4. Load session from database using flow ID
5. Compare hashed device ID with `session.device_id`
6. Allow request if match, throw `BaseException.unauthorized()` if mismatch

### 3. Optional Device ID

For endpoints where device ID is optional:

```java
@PostMapping("/some-endpoint")
@RequiresDeviceId(mode = DeviceValidationMode.EXISTENCE_ONLY, required = false)
public ResponseEntity<?> someMethod() {
    // If device ID present, validates it
    // If device ID absent, skips validation
}
```

## Applied Endpoints

### EXISTENCE_ONLY Mode
- `POST /identifier/backoffice` - Backoffice user identification
- `POST /identifier/mobile` - Mobile user identification
- `POST /identifier/internet-banking` - Internet banking user identification

### SESSION_BOUND Mode
- `POST /password` - Password authentication
- `POST /mfa/initiate` - MFA initiation
- `POST /mfa/verify` - MFA verification
- `POST /password/change` - Password change
- `POST /security-questions` - Security questions submission
- `POST /token` - Token refresh

## Security Features

### 1. Device Fingerprinting
- Device ID stored as SHA-256 hash
- Cookie name: `__Host-SBS_SID` (secure prefix)
- Attributes: `HttpOnly`, `Secure`, `SameSite=Strict`

### 2. Session Binding
- Each session tracks the device ID used during authentication
- Token refresh requires same device that initiated authentication
- Prevents token theft and replay attacks

### 3. Audit Logging
- All validation failures logged with device ID prefix (first 8 chars)
- Session mismatches trigger security warnings
- Suspicious activity can be tracked through logs

### 4. Attack Prevention
- **Token Theft**: SESSION_BOUND prevents stolen tokens from working on different devices
- **Session Hijacking**: Device mismatch immediately detected
- **Replay Attacks**: Device binding ensures requests come from authenticated device

## Configuration

### Interceptor Registration

The interceptor is registered in `WebConfig.java`:

```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    // Device ID validation runs before stage checks
    registry.addInterceptor(deviceIdInterceptor);
    registry.addInterceptor(stageCheckInterceptor);
}
```

### Cookie Configuration

Device ID cookie settings in `application.yml`:

```yaml
shiva:
  security:
    cookies:
      same-site: Strict
      secure: true
      http-only: true
```

## Error Handling

All validation failures throw `BaseException.unauthorized()` with appropriate messages:

- `"Device identification required"` - Device ID missing but required
- `"Invalid device"` - Device not found or inactive
- `"Invalid session"` - Session not found
- `"Device mismatch"` - Device doesn't match session
- `"Invalid request context"` - Flow ID missing for SESSION_BOUND

## Database Schema

### devices table
```sql
- device_id (VARCHAR, SHA-256 hash, indexed)
- active (BOOLEAN)
- device_type, platform, browser (metadata)
- first_ip, last_ip (tracking)
- first_seen_at, last_seen_at (timestamps)
```

### sessions table
```sql
- id (BIGINT, primary key)
- session_id (VARCHAR, UUID as string)
- device_id (VARCHAR, SHA-256 hash)
- iam_user_id (foreign key)
- status (enum: LoginStage)
- session_type (enum: SessionType)
```

## Testing Recommendations

### Unit Tests
1. Test EXISTENCE_ONLY with valid/invalid/missing device IDs
2. Test SESSION_BOUND with matching/mismatching device IDs
3. Test optional device ID behavior
4. Test error messages and logging

### Integration Tests
1. Full authentication flow with device validation
2. Token refresh with correct/incorrect device
3. Multiple sessions from different devices
4. Device revocation scenarios

### Security Tests
1. Attempt token refresh from different device
2. Try password auth without device cookie
3. Verify device ID cannot be spoofed
4. Test session hijacking scenarios

## Migration Guide

### From Manual Validation

**Before:**
```java
@PostMapping("/password")
public ResponseEntity<?> passwordStep(
    @CookieValue(value = SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME) String deviceId,
    @FlowId UUID flowId
) {
    // Manual validation in controller
    if (!deviceService.validateDevice(deviceId, flowId)) {
        throw BaseException.unauthorized("Invalid device");
    }
    // Implementation
}
```

**After:**
```java
@PostMapping("/password")
@RequiresDeviceId(mode = DeviceValidationMode.SESSION_BOUND)
public ResponseEntity<?> passwordStep(@FlowId UUID flowId) {
    // Device validation handled by interceptor
    // No need for @CookieValue parameter
    // Implementation
}
```

### Benefits
- ✅ Declarative security at method level
- ✅ Consistent validation logic across all endpoints
- ✅ Reduced boilerplate code
- ✅ Centralized audit logging
- ✅ Easy to add/remove validation from endpoints
- ✅ Clear security requirements visible in code

## Troubleshooting

### Device ID Not Found
**Symptom:** `"Invalid device"` error on identifier lookup  
**Solution:** Ensure `/device/init` was called first to register device

### Session Not Found
**Symptom:** `"Invalid session"` error on SESSION_BOUND endpoints  
**Solution:** Verify flow ID header is present and valid

### Device Mismatch
**Symptom:** `"Device mismatch"` error on authenticated operations  
**Solution:** User switched devices mid-flow, restart authentication

### Missing Flow ID
**Symptom:** `"Invalid request context"` error  
**Solution:** Ensure X-Flow-ID header is sent with SESSION_BOUND requests

## Future Enhancements

1. **Multi-Device Support**: Allow users to register multiple devices
2. **Device Risk Scoring**: Track suspicious device behavior
3. **Geo-Location Validation**: Verify device location consistency
4. **Device Revocation API**: Allow users to deactivate devices
5. **Push Notifications**: Alert users of new device logins
6. **Header-Based Device ID**: Support X-Device-ID header for mobile apps
7. **Rate Limiting per Device**: Prevent brute force from specific devices

## Comparison to Laravel Middleware

| Feature | Laravel | Spring Boot (This Implementation) |
|---------|---------|-----------------------------------|
| Declaration | Route middleware | Method annotation |
| Execution | Before/After controller | Before controller (preHandle) |
| Configuration | Kernel registration | WebConfig registration |
| Parameters | Route parameters | Annotation attributes |
| Customization | Middleware classes | Validation modes enum |
| Reusability | High | High |

## Related Documentation

- [PASSWORD_DECRYPTION_SUMMARY.md](PASSWORD_DECRYPTION_SUMMARY.md)
- [RATE_LIMITING_IMPLEMENTATION_STATUS.md](RATE_LIMITING_IMPLEMENTATION_STATUS.md)
- [IAM_SERVICE_DOCUMENTATION.md](IAM_SERVICE_DOCUMENTATION.md)

## Contributors

Implementation completed: January 15, 2026

