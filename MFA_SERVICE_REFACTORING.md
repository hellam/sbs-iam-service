# MFA Service Refactoring - Implementation Guide

## Overview

Refactored the MFA (Multi-Factor Authentication) service architecture to support both **login flow** and **forgot password flow** by extracting common MFA logic into a shared service.

## Problem Statement

### Before Refactoring

`ForgotPasswordMfaService` was attempting to reuse `MfaService`, but this caused issues because:

1. **Tight Coupling**: `MfaService` was tightly coupled to `LoginFlowService` and `LoginRequirements`
2. **Wrong Flow Context**: `MfaService` expected login session stages (`PASSWORD_OK`) but forgot password uses different stages (`FP_IDENTIFIER_OK`, `FP_SECURITY_QUESTIONS_OK`)
3. **Incorrect Stage Updates**: `MfaService` would update to `MFA_OK` instead of `FP_MFA_OK`
4. **Different Requirements**: Login requirements model doesn't match forgot password requirements

## Solution Architecture

### Three-Layer MFA Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Flow-Specific Services                 │
├──────────────────────────┬──────────────────────────────┤
│     MfaService           │  ForgotPasswordMfaService    │
│  (Login Flow)            │  (Forgot Password Flow)      │
│                          │                              │
│ - LoginFlowService       │ - ForgotPasswordFlowService  │
│ - LoginRequirements      │ - ForgotPasswordRequirements │
│ - Stage: PASSWORD_OK     │ - Stages: FP_IDENTIFIER_OK   │
│ - Updates to: MFA_OK     │ - Updates to: FP_MFA_OK      │
└──────────┬───────────────┴───────────┬──────────────────┘
           │                           │
           │    ┌─────────────────┐    │
           └───►│ CommonMfaService│◄───┘
                └─────────────────┘
                        │
           ┌────────────┴────────────┐
           │                         │
      ┌────▼─────┐            ┌─────▼────┐
      │OtpService│            │TotpVerifier│
      └──────────┘            └──────────┘
```

## Implementation Details

### 1. CommonMfaService (New)

**Purpose**: Provides core MFA operations (OTP/TOTP) without flow-specific logic

**Key Methods**:
```java
// Send OTP via notification channel
void sendOtp(SessionEntity session, NotificationChannel channel)

// Verify OTP code
boolean verifyOtp(String sessionId, String code)

// Verify TOTP code
boolean verifyTotp(IamUserEntity user, String code)

// Verify MFA code (auto-detects OTP or TOTP)
boolean verifyMfaCode(SessionEntity session, String code, boolean isTotpRequired)

// Validate and throw exception if invalid
void validateMfaCode(SessionEntity session, String code, boolean isTotpRequired)
```

**Dependencies**:
- `OtpService` - For OTP generation and verification
- `TotpVerifier` - For TOTP verification

**No Flow Dependencies**: Does NOT depend on LoginFlowService or ForgotPasswordFlowService

### 2. MfaService (Refactored)

**Purpose**: Handles MFA for login flow

**Before**:
```java
@Service
public class MfaService {
    private final OtpService otpService;
    private final TotpVerifier totpVerifier;
    // ... direct usage of OtpService and TotpVerifier
}
```

**After**:
```java
@Service
public class MfaService {
    private final LoginFlowService loginFlowService;
    private final CommonMfaService commonMfaService;  // ← Uses common service
    private final SecurityEventService securityEventService;
    private final LoginHistoryService loginHistoryService;
}
```

**What Changed**:
- ✅ Now uses `CommonMfaService` instead of directly using `OtpService` and `TotpVerifier`
- ✅ Maintains login flow-specific logic (stage validation, event logging)
- ✅ Uses `LoginRequirements` to determine OTP vs TOTP
- ✅ Updates to correct stage: `MFA_OK`

### 3. ForgotPasswordMfaService (Refactored)

**Purpose**: Handles MFA for forgot password flow

**Before**:
```java
@Service
public class ForgotPasswordMfaService {
    private final ForgotPasswordFlowService flowService;
    private final MfaService mfaService;  // ← Wrong! Login flow service
    // ... trying to use login flow MFA service
}
```

**After**:
```java
@Service
public class ForgotPasswordMfaService {
    private final ForgotPasswordFlowService flowService;
    private final CommonMfaService commonMfaService;  // ← Uses common service
    private final ObjectMapper objectMapper;
}
```

**What Changed**:
- ✅ Now uses `CommonMfaService` instead of `MfaService`
- ✅ Maintains forgot password flow-specific logic
- ✅ Uses `ForgotPasswordRequirements` to check if MFA is required
- ✅ Updates to correct stage: `FP_MFA_OK`
- ✅ Only supports OTP (not TOTP) for security reasons
- ✅ No profile selection in response (forgot password doesn't need it)

## Key Design Decisions

### 1. Why Not Reuse MfaService Directly?

| Aspect | If Reused MfaService | With CommonMfaService |
|--------|---------------------|----------------------|
| Flow Validation | ❌ Wrong flow service | ✅ Each flow manages its own |
| Stage Validation | ❌ Expects PASSWORD_OK | ✅ Each flow validates correctly |
| Stage Updates | ❌ Updates to MFA_OK | ✅ Updates to correct stage |
| Requirements | ❌ Uses LoginRequirements | ✅ Uses correct requirements |
| Event Logging | ❌ Login-specific events | ✅ Each flow logs appropriately |

### 2. Why Only OTP for Forgot Password?

**Security Consideration**: In forgot password flow, user might have lost access to their TOTP device. Only OTP (via SMS/Email) ensures they can still recover their account using verified contact information.

```java
// ForgotPasswordMfaService only uses OTP
commonMfaService.sendOtp(session, request.getChannel());
boolean valid = commonMfaService.verifyOtp(session.getSessionId(), request.getCode());

// Login MfaService supports both OTP and TOTP
boolean ok = commonMfaService.verifyMfaCode(session, req.getCode(), reqs.isTotpRequired());
```

### 3. Separation of Concerns

```
CommonMfaService:
├─ Core MFA operations (OTP/TOTP)
├─ No flow-specific logic
└─ Reusable across different flows

MfaService:
├─ Login flow-specific validation
├─ Login stage management
├─ Login event logging
└─ Uses CommonMfaService for core operations

ForgotPasswordMfaService:
├─ Forgot password flow-specific validation
├─ Forgot password stage management
├─ OTP-only (security decision)
└─ Uses CommonMfaService for core operations
```

## Usage Examples

### Login Flow MFA

```java
// 1. Initiate MFA
POST /api/v1/mfa/initiate
Headers: X-Flow-ID: {loginFlowId}
Body: {"channel": "SMS"}

// Behind the scenes:
- MfaService validates login stage (PASSWORD_OK)
- Uses CommonMfaService.sendOtp()
- Returns flowId

// 2. Verify MFA
POST /api/v1/mfa/verify
Headers: X-Flow-ID: {loginFlowId}
Body: {"code": "123456"}

// Behind the scenes:
- MfaService validates login stage
- Uses CommonMfaService.verifyMfaCode()
- Updates to MFA_OK
- Logs security events
```

### Forgot Password Flow MFA

```java
// 1. Initiate MFA
POST /api/v1/forgot-password/mfa/initiate
Headers: X-Flow-ID: {forgotPasswordFlowId}
Body: {"channel": "EMAIL"}

// Behind the scenes:
- ForgotPasswordMfaService validates forgot password stage
- Uses CommonMfaService.sendOtp()
- Returns flowId

// 2. Verify MFA
POST /api/v1/forgot-password/mfa/verify
Headers: X-Flow-ID: {forgotPasswordFlowId}
Body: {"code": "123456"}

// Behind the scenes:
- ForgotPasswordMfaService validates forgot password stage
- Uses CommonMfaService.verifyOtp()
- Updates to FP_MFA_OK
- No event logging (forgot password context)
```

## Benefits of This Architecture

### 1. Code Reusability
✅ Core MFA logic in one place
✅ No duplication of OTP/TOTP logic
✅ Easy to maintain and test

### 2. Clear Separation of Concerns
✅ Flow-specific logic stays in flow services
✅ Common logic in common service
✅ Single Responsibility Principle

### 3. Extensibility
✅ Easy to add new flows (e.g., password reset by admin)
✅ Easy to add new MFA methods (e.g., biometric)
✅ Easy to modify OTP/TOTP logic without touching flow logic

### 4. Testability
✅ Can test CommonMfaService independently
✅ Can mock CommonMfaService in flow service tests
✅ Clear boundaries for unit tests

### 5. Maintainability
✅ Changes to OTP logic only affect CommonMfaService
✅ Changes to flow logic don't affect other flows
✅ Easy to understand and modify

## Files Modified

### Created (1 file)
1. **`CommonMfaService.java`** - Common MFA operations service

### Modified (2 files)
1. **`MfaService.java`** - Now uses CommonMfaService
2. **`ForgotPasswordMfaService.java`** - Now uses CommonMfaService directly

## Migration Notes

### For Developers

If you need to add a new flow that requires MFA:

1. ✅ **DO** use `CommonMfaService` for core MFA operations
2. ✅ **DO** implement flow-specific validation in your flow service
3. ✅ **DO** update to your flow-specific stages
4. ❌ **DON'T** try to reuse `MfaService` (it's login-specific)
5. ❌ **DON'T** duplicate OTP/TOTP logic

### Example: Adding MFA to a new "Change Email" flow

```java
@Service
public class ChangeEmailMfaService {
    private final ChangeEmailFlowService flowService;
    private final CommonMfaService commonMfaService;  // ← Reuse common service
    
    public void initiate(UUID flowId) {
        SessionEntity session = flowService.requireStage(flowId, ChangeEmailStage.EMAIL_ENTERED);
        commonMfaService.sendOtp(session, NotificationChannel.EMAIL);
    }
    
    public void verify(UUID flowId, String code) {
        SessionEntity session = flowService.requireStage(flowId, ChangeEmailStage.EMAIL_ENTERED);
        commonMfaService.validateMfaCode(session, code, false);  // OTP only
        flowService.updateStage(session, ChangeEmailStage.MFA_VERIFIED);
    }
}
```

## Testing

### Unit Tests for CommonMfaService

```java
@Test
void testSendOtp() {
    // Given
    SessionEntity session = createTestSession();
    NotificationChannel channel = NotificationChannel.SMS;
    
    // When
    commonMfaService.sendOtp(session, channel);
    
    // Then
    verify(otpService).sendOtp(session, channel);
}

@Test
void testVerifyOtp() {
    // Given
    String sessionId = "test-session";
    String code = "123456";
    when(otpService.verify(sessionId, code)).thenReturn(true);
    
    // When
    boolean result = commonMfaService.verifyOtp(sessionId, code);
    
    // Then
    assertTrue(result);
}
```

### Integration Tests

```java
@Test
void testLoginFlowMfa() {
    // Test that login flow MFA works correctly
    // with CommonMfaService integration
}

@Test
void testForgotPasswordFlowMfa() {
    // Test that forgot password flow MFA works correctly
    // with CommonMfaService integration
}
```

## Conclusion

The refactored MFA architecture provides:
- ✅ Clear separation between flow-specific and common logic
- ✅ Reusable MFA operations across different flows
- ✅ Maintainable and testable code
- ✅ Easy to extend for future flows

Both login and forgot password flows now use the same underlying MFA infrastructure while maintaining their specific requirements and validation logic.
