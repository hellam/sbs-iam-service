# Account Lockout Service Implementation

## Overview
The `AccountLockoutService` is a dynamic, policy-based service that manages account lockout functionality for both Customer and Employee authentication entities across multiple channels (Internet Banking, Mobile Banking, and Backoffice).

## Date Created
January 13, 2026

## Architecture

### Design Principles
1. **DRY (Don't Repeat Yourself)**: Centralized lockout logic that can be reused across multiple services
2. **Policy-Driven**: All lockout behavior is controlled by `PasswordPolicyEntity` configuration
3. **Channel-Aware**: Different channels can have different lockout policies
4. **Automatic Unlock**: Expired lockouts are automatically cleared during authentication attempts
5. **Transaction Safety**: Uses `REQUIRES_NEW` propagation to ensure lockout state is persisted independently

## Features

### 1. Multi-Entity Support
Handles both authentication entity types:
- **CustomerAuthEntity**: Supports Internet Banking and Mobile Banking with separate lockout states
- **EmployeeAuthEntity**: Supports Backoffice authentication

### 2. Channel-Specific Lockout
Each channel maintains its own lockout state:
- **Internet Banking**: `internetLocked`, `internetLockoutUntil`, `internetFailedAttempts`
- **Mobile Banking**: `mobileLocked`, `mobileLockoutUntil`, `mobileFailedAttempts`
- **Backoffice**: `staffLocked`, `staffLockoutUntil`, `staffFailedAttempts`

### 3. Policy-Based Configuration
Lockout behavior is controlled by `PasswordPolicyEntity`:
```java
- maxFailedAttempts: Number of failed attempts before lockout
- lockoutMinutes: Duration of lockout (null = permanent)
```

### 4. Automatic Unlock
When a locked account attempts authentication:
- If `lockoutUntil` is null → Permanent lock (requires manual intervention)
- If `lockoutUntil` has passed → Automatically unlocks and resets failed attempts
- If `lockoutUntil` is in the future → Throws `accountLocked` exception

## API Methods

### Validation Methods

#### `checkAndValidateCustomerLockout(IamUserEntity user, Channel channel)`
Checks if customer account is locked for the specified channel (INTERNET_BANKING or MOBILE_BANKING).
- **Throws**: `BaseException.accountLocked()` if account is still locked
- **Side Effect**: Automatically unlocks if lockout period has expired

#### `checkAndValidateEmployeeLockout(IamUserEntity user, Channel channel)`
Checks if employee account is locked for backoffice access.
- **Throws**: `BaseException.accountLocked()` if account is still locked
- **Side Effect**: Automatically unlocks if lockout period has expired

### Recording Failed Attempts

#### `recordCustomerInternetFailedAttempt(IamUserEntity user, Channel channel)`
Increments internet banking failed attempts counter.
- Locks account if threshold (`maxFailedAttempts`) is reached
- Sets `lockoutUntil` based on policy configuration

#### `recordCustomerMobileFailedAttempt(IamUserEntity user, Channel channel)`
Increments mobile banking failed attempts counter.
- Locks account if threshold is reached
- Sets `lockoutUntil` based on policy configuration

#### `recordEmployeeFailedAttempt(IamUserEntity user, Channel channel)`
Increments backoffice failed attempts counter.
- Locks account if threshold is reached
- Sets `lockoutUntil` based on policy configuration

### Resetting Lockout

#### `resetCustomerInternetLockout(IamUserEntity user)`
Resets internet banking lockout state after successful authentication:
- Sets `internetLocked = false`
- Clears `internetLockoutUntil`
- Resets `internetFailedAttempts = 0`

#### `resetCustomerMobileLockout(IamUserEntity user)`
Resets mobile banking lockout state after successful authentication:
- Sets `mobileLocked = false`
- Clears `mobileLockoutUntil`
- Resets `mobileFailedAttempts = 0`

#### `resetEmployeeLockout(IamUserEntity user)`
Resets backoffice lockout state after successful authentication:
- Sets `staffLocked = false`
- Clears `staffLockoutUntil`
- Resets `staffFailedAttempts = 0`

### Additional Methods

#### `lockAccountForOtpFailure(IamUserEntity user, Channel channel)`
Locks account due to excessive failed OTP verification attempts.
- Can be called by `OtpService` when OTP verification fails repeatedly
- Uses same policy lockout duration as password failures

## Integration

### PasswordVerifier Integration

The `PasswordVerifier` service has been refactored to use `AccountLockoutService`:

**Before**: Direct entity manipulation with duplicated lockout logic
**After**: Delegated to `AccountLockoutService` for cleaner separation of concerns

```java
// Check lockout before verification
accountLockoutService.checkAndValidateCustomerLockout(user, channel);

// Verify password
boolean matches = HashUtil.bcryptVerify(rawPassword, passwordHash);

if (matches) {
    // Reset on success
    accountLockoutService.resetCustomerInternetLockout(user);
} else {
    // Record failure and lock if needed
    accountLockoutService.recordCustomerInternetFailedAttempt(user, channel);
}
```

### OtpService Integration (Available)

The `OtpService` can optionally use `AccountLockoutService` to lock accounts after too many failed OTP attempts:

```java
// After max OTP verification failures
if (otpRecord.getVerifyAttempts() >= mfaPolicy.getMaxVerifyAttempts()) {
    accountLockoutService.lockAccountForOtpFailure(session.getIamUser(), session.getChannel());
    otpRecord.setStatus("BLOCKED");
}
```

## Database Schema

### CustomerAuthEntity
```sql
-- Internet Banking
internet_locked BOOLEAN DEFAULT false
internet_lockout_until TIMESTAMP WITH TIME ZONE
internet_failed_attempts SMALLINT DEFAULT 0

-- Mobile Banking
mobile_locked BOOLEAN DEFAULT false
mobile_lockout_until TIMESTAMP WITH TIME ZONE
mobile_failed_attempts SMALLINT DEFAULT 0
```

### EmployeeAuthEntity
```sql
-- Backoffice
staff_locked BOOLEAN DEFAULT false
staff_lockout_until TIMESTAMP WITH TIME ZONE
staff_failed_attempts SMALLINT DEFAULT 0
```

## Transaction Management

All public methods use `@Transactional(propagation = Propagation.REQUIRES_NEW)`:
- Ensures lockout state is persisted even if calling transaction rolls back
- Prevents race conditions during concurrent authentication attempts
- Guarantees atomic lockout operations

## Error Handling

### Custom Exceptions
- `BaseException.iamUserCredentialsNotFound()`: Thrown when authentication entity doesn't exist
- `BaseException.accountLocked()`: Thrown when account is locked and lockout hasn't expired

### Lock Messages
- **Temporary Lock**: "Account is locked. Please try again later or contact support."
- **Permanent Lock**: "Account is permanently locked. Please contact support."

## Configuration Example

### Password Policy (in database)
```json
{
  "channel": "INTERNET_BANKING",
  "maxFailedAttempts": 5,
  "lockoutMinutes": 30,
  "description": "Lock account for 30 minutes after 5 failed login attempts"
}
```

### Policy Variations
```json
// Permanent lock until manual reset
{
  "maxFailedAttempts": 3,
  "lockoutMinutes": null
}

// No lockout (for testing/development)
{
  "maxFailedAttempts": 999999,
  "lockoutMinutes": 0
}

// Strict lockout for sensitive operations
{
  "maxFailedAttempts": 3,
  "lockoutMinutes": 60
}
```

## Testing Scenarios

### 1. Temporary Lockout
```
1. User fails login 5 times (maxFailedAttempts = 5)
2. Account locked for 30 minutes
3. User waits 30 minutes
4. Next login attempt automatically unlocks account
5. User can login successfully
```

### 2. Permanent Lockout
```
1. User fails login 3 times (maxFailedAttempts = 3, lockoutMinutes = null)
2. Account permanently locked
3. Any login attempt throws accountLocked exception
4. Requires admin intervention to unlock
```

### 3. Successful Login Reset
```
1. User fails login 3 times
2. User enters correct password on 4th attempt
3. Failed attempts counter reset to 0
4. Lockout state cleared
```

### 4. Channel Isolation
```
1. User fails Internet Banking login 5 times → Internet locked
2. User can still access Mobile Banking (separate counter)
3. Each channel maintains independent lockout state
```

## Benefits

### 1. Code Reusability
Single service handles lockout for:
- Password verification
- OTP verification (future)
- Admin password reset flows
- Any authentication mechanism

### 2. Maintainability
- Centralized lockout logic (one place to update)
- Policy-driven (no code changes for policy adjustments)
- Clear separation of concerns

### 3. Security
- Prevents brute-force attacks
- Configurable thresholds per channel
- Automatic unlock prevents DoS on legitimate users
- Transaction safety prevents race conditions

### 4. Flexibility
- Different policies per channel
- Temporary or permanent locks
- Can be extended for additional channels
- Supports multiple entity types

## Future Enhancements

1. **Lockout Events**: Emit events when accounts are locked for monitoring/alerting
2. **Admin Unlock API**: Add methods for admins to manually unlock accounts
3. **Lockout History**: Track lockout events in separate audit table
4. **Progressive Delays**: Implement exponential backoff between attempts
5. **IP-Based Lockout**: Lock based on IP address patterns
6. **Notification**: Send email/SMS when account is locked
7. **Metrics**: Expose lockout metrics for monitoring dashboards

## Related Files

- `AccountLockoutService.java`: Main service implementation
- `PasswordVerifier.java`: Uses the service for password verification
- `OtpService.java`: Can optionally use for OTP verification
- `CustomerAuthEntity.java`: Customer authentication state
- `EmployeeAuthEntity.java`: Employee authentication state
- `PasswordPolicyEntity.java`: Policy configuration

## Version History

### v1.0.0 (January 13, 2026)
- Initial implementation
- Support for Customer (Internet/Mobile) and Employee (Backoffice)
- Policy-based lockout configuration
- Automatic unlock on expiry
- Transaction-safe operations

