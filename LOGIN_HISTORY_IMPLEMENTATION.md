# Login History Implementation Summary

## Overview
Comprehensive login history tracking has been implemented across the entire authentication flow. Every login attempt from identifier verification to final authentication is now logged in the `LoginHistoryEntity`.

## Files Created

### 1. LoginHistoryRepository.java
**Location:** `/services/iam-service/src/main/java/ke/shiva/sbs_iam/modules/iam/infra/repository/LoginHistoryRepository.java`

Spring Data JPA repository for accessing login history records with methods to:
- Find all login attempts by user
- Find successful login attempts by user
- Find login attempts after a specific date

### 2. RequestContextExtractor.java
**Location:** `/services/iam-service/src/main/java/ke/shiva/sbs_iam/modules/iam/app/util/RequestContextExtractor.java`

Utility component that extracts contextual information from HTTP requests:
- **IP Address**: Extracts real IP from various proxy headers (X-Forwarded-For, X-Real-IP, etc.)
- **User Agent**: Captures browser/device information
- **Device ID**: Extracts custom device identifier from headers
- **Location**: Captures country and city from headers (for GeoIP integration)

### 3. LoginHistoryService.java
**Location:** `/services/iam-service/src/main/java/ke/shiva/sbs_iam/modules/iam/app/service/LoginHistoryService.java`

Core service for logging all login attempts with methods:
- `logIdentifierSuccess()` - Logs successful identifier verification
- `logIdentifierFailure()` - Logs failed identifier verification (user not found, inactive)
- `logPasswordSuccess()` - Logs successful password authentication
- `logPasswordFailure()` - Logs failed password authentication
- `logMfaSuccess()` - Logs successful MFA verification
- `logMfaFailure()` - Logs failed MFA verification
- `logLoginSuccess()` - Logs complete successful login
- `logLoginFailure()` - Logs general login failure

**Key Features:**
- Uses `@Transactional(propagation = Propagation.REQUIRES_NEW)` to ensure login history is saved even if the main transaction fails
- Automatically extracts request context (IP, user agent, device ID, location)
- Handles null values gracefully with try-catch blocks to prevent logging failures from affecting authentication

## Files Modified

### 4. IdentifierService.java
**Changes:**
- Added `LoginHistoryService` dependency
- Logs successful identifier verification with `logIdentifierSuccess()`
- Logs failed identifier verification with `logIdentifierFailure()` for:
  - Unknown identifiers (IDENTIFIER_NOT_FOUND)
  - Inactive users (USER_INACTIVE)
- Stores the identifier in session metadata for use in later stages

### 5. PasswordAuthService.java
**Changes:**
- Added `LoginHistoryService` dependency
- Logs successful password authentication with `logPasswordSuccess()`
- Logs failed password attempts with `logPasswordFailure(PASSWORD_INVALID)`
- Extracts identifier from session metadata

### 6. MfaService.java
**Changes:**
- Added `LoginHistoryService` dependency
- Logs successful MFA verification with `logMfaSuccess()`
- Logs failed MFA attempts with `logMfaFailure(MFA_INVALID)`
- Extracts identifier from session metadata

### 7. FinalizeLoginController.java
**Changes:**
- Added `LoginHistoryService` dependency
- Logs successful complete login with `logLoginSuccess()` when user finalizes login without profile selection
- Extracts identifier from session metadata

### 8. ProfileService.java
**Changes:**
- Added `LoginHistoryService` dependency
- Logs successful complete login with `logLoginSuccess()` when user selects a profile (for Internet Banking)
- Extracts identifier from session metadata

## Authentication Flow with Login History

### Step 1: Identifier Verification
**Endpoint:** `POST /oauth/identifier/{channel}`
- ✅ **Success**: Logs with `success=false`, `failure_reason=PENDING_PASSWORD_AUTH`
- ❌ **Failure**: Logs with `success=false`, `failure_reason=IDENTIFIER_NOT_FOUND` or `USER_INACTIVE`

### Step 2: Password Authentication
**Endpoint:** `POST /oauth/password`
- ✅ **Success**: Logs with `success=false`, `failure_reason=PENDING_MFA`
- ❌ **Failure**: Logs with `success=false`, `failure_reason=PASSWORD_INVALID`

### Step 3: MFA Verification (Optional)
**Endpoint:** `POST /oauth/mfa/verify`
- ✅ **Success**: Logs with `success=false`, `failure_reason=PENDING_FINALIZATION`
- ❌ **Failure**: Logs with `success=false`, `failure_reason=MFA_INVALID`

### Step 4a: Finalize Without Profile (Mobile/Backoffice)
**Endpoint:** `POST /oauth/finalize`
- ✅ **Success**: Logs with `success=true`, `failure_reason=null`

### Step 4b: Profile Selection (Internet Banking)
**Endpoint:** `POST /oauth/profiles/select`
- ✅ **Success**: Logs with `success=true`, `failure_reason=null`

## Data Captured in LoginHistoryEntity

For each login attempt, the following information is captured:
- **iam_user_id**: Reference to the user (null for identifier failures)
- **channel**: Channel used (MOBILE_BANKING, INTERNET_BANKING, BACKOFFICE)
- **identifier_used**: The identifier used for login (username, email, phone)
- **success**: Boolean indicating if login was completely successful
- **failure_reason**: Reason for failure or pending status
- **ip_address**: Real IP address of the client
- **device_id**: Device identifier from request header
- **user_agent**: Browser/app user agent string
- **location_country**: Country code (if provided by GeoIP service)
- **location_city**: City name (if provided by GeoIP service)
- **created_at**: Timestamp of the login attempt

## Benefits

1. **Complete Audit Trail**: Every step of the authentication process is logged
2. **Security Monitoring**: Failed login attempts can be monitored for suspicious activity
3. **User Activity Tracking**: Users can see their login history
4. **Compliance**: Meets regulatory requirements for authentication logging
5. **Troubleshooting**: Helps diagnose authentication issues
6. **Risk Analysis**: IP addresses and device IDs can be used for risk-based authentication

## Usage Examples

### Query Recent Failed Login Attempts
```java
List<LoginHistoryEntity> failedAttempts = loginHistoryRepository
    .findByIamUserAndCreatedAtAfterOrderByCreatedAtDesc(
        user, 
        OffsetDateTime.now().minusHours(24)
    );
```

### Query Successful Logins
```java
List<LoginHistoryEntity> successfulLogins = loginHistoryRepository
    .findByIamUserAndSuccessTrueOrderByCreatedAtDesc(user);
```

## Security Considerations

1. **Transaction Isolation**: Uses `REQUIRES_NEW` propagation to ensure logs are saved even if authentication fails
2. **Error Handling**: All logging is wrapped in try-catch to prevent logging failures from breaking authentication
3. **IP Detection**: Checks multiple proxy headers to get real client IP
4. **Data Privacy**: User agent strings are truncated to 500 characters max
5. **Minimal Impact**: Logging happens asynchronously in separate transactions

## Future Enhancements

1. **GeoIP Integration**: Add service to resolve IP addresses to geographic locations
2. **Risk Scoring**: Calculate risk scores based on IP, device, location changes
3. **Alerting**: Send alerts for suspicious login patterns
4. **User Notifications**: Notify users of logins from new devices/locations
5. **Login History API**: Create endpoints for users to view their login history
6. **Analytics Dashboard**: Build admin dashboard to visualize login trends

## Testing Recommendations

1. Test identifier verification logging (success and failure cases)
2. Test password authentication logging (success and failure cases)
3. Test MFA verification logging (success and failure cases)
4. Test complete login flow logging
5. Test profile selection flow logging
6. Verify IP address extraction with various proxy headers
7. Verify transaction isolation (logs saved even when auth fails)
8. Test with null/missing request context
9. Load test to ensure logging doesn't impact performance
10. Verify database constraints and indexes on login_history table

## Database Migration Required

Ensure the `login_history` table exists with proper schema:
```sql
CREATE TABLE iam_service.login_history (
    id BIGSERIAL PRIMARY KEY,
    iam_user_id BIGINT REFERENCES iam_service.iam_users(id),
    channel VARCHAR(50),
    identifier_used VARCHAR(255),
    success BOOLEAN DEFAULT false,
    failure_reason VARCHAR(100),
    ip_address VARCHAR(50),
    device_id VARCHAR(255),
    user_agent TEXT,
    location_country VARCHAR(2),
    location_city VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_login_history_user_created ON iam_service.login_history(iam_user_id, created_at DESC);
CREATE INDEX idx_login_history_ip ON iam_service.login_history(ip_address);
CREATE INDEX idx_login_history_success ON iam_service.login_history(success, created_at DESC);
```

