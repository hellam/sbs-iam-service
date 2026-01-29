# Forgot Password Implementation

## Overview
A complete forgot password flow implementation following the same architecture patterns as the login flow. The flow supports multi-step verification including security questions and MFA based on configured policies.

## Architecture

### Flow Stages
The forgot password flow uses dedicated stages in the `LoginStage` enum:
- `FP_IDENTIFIER_OK` - Identifier verified
- `FP_SECURITY_QUESTIONS_OK` - Security questions answered correctly
- `FP_MFA_OK` - MFA verification completed
- `FP_PASSWORD_RESET` - Password reset completed

### Session Type
- `FORGOT_PASSWORD_TEMP` - Temporary session for forgot password flow (15-minute expiry)

## API Endpoints

### 1. Initiate Forgot Password
**POST** `/api/v1/forgot-password/identifier/{channel}`

Channels:
- `/identifier/backoffice`
- `/identifier/mobile`
- `/identifier/internet-banking`

**Request:**
```json
{
  "identifier": "user@example.com"
}
```

**Response:**
```json
{
  "flowId": "uuid",
  "securityQuestionsRequired": true,
  "securityQuestionsCount": 2,
  "mfaRequired": true,
  "nextStep": "SECURITY_QUESTIONS|MFA|RESET_PASSWORD"
}
```

### 2. Verify Security Questions (Optional)
**POST** `/api/v1/forgot-password/security-questions/verify`

**Headers:**
- `X-Flow-ID: {flowId}`

**Request:**
```json
{
  "flowId": "uuid",
  "answers": [
    {
      "questionId": 1,
      "answer": "My answer"
    }
  ]
}
```

**Response:**
```json
{
  "verified": true,
  "nextStep": "MFA|RESET_PASSWORD"
}
```

### 3. Initiate MFA (Optional)
**POST** `/api/v1/forgot-password/mfa/initiate`

**Headers:**
- `X-Flow-ID: {flowId}`

**Request:**
```json
{
  "channel": "SMS|EMAIL"
}
```

**Response:**
```json
{
  "otpSent": true,
  "maskedDestination": "+254***123",
  "expiresIn": 120
}
```

### 4. Verify MFA (Optional)
**POST** `/api/v1/forgot-password/mfa/verify`

**Headers:**
- `X-Flow-ID: {flowId}`

**Request:**
```json
{
  "otp": "123456"
}
```

**Response:**
```json
{
  "flowId": "uuid",
  "nextIsProfileSelection": false
}
```

### 5. Reset Password
**POST** `/api/v1/forgot-password/reset`

**Headers:**
- `X-Flow-ID: {flowId}`

**Request:**
```json
{
  "newPassword": "NewSecureP@ssw0rd",
  "confirmPassword": "NewSecureP@ssw0rd"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password has been reset successfully. You can now log in with your new password."
}
```

## Flow Logic

### Step 1: Identifier Verification
1. User provides identifier (email, phone, username)
2. System looks up user by identifier and channel
3. System evaluates forgot password requirements based on policies:
   - Check if security questions are required (`askOnForgotPassword` in security question policy)
   - Check if MFA is required (based on MFA policy)
4. System creates temporary session with 15-minute expiry
5. Returns next step based on requirements

### Step 2: Security Questions (Conditional)
**Required when:**
- Security question policy has `askOnForgotPassword = true`
- User has previously set up security questions

**Process:**
1. User answers the configured number of security questions
2. System verifies answers match stored hashed answers
3. Updates session stage to `FP_SECURITY_QUESTIONS_OK`
4. Returns next step (MFA or RESET_PASSWORD)

### Step 3: MFA Verification (Conditional)
**Required when:**
- MFA policy is enabled for the channel

**Process:**
1. User initiates MFA (selects channel: SMS/EMAIL)
2. System sends OTP to user's registered contact
3. User submits OTP
4. System verifies OTP
5. Updates session stage to `FP_MFA_OK`
6. Returns success response

### Step 4: Password Reset
**Validation:**
- All required steps must be completed:
  - If security questions required → must be at `FP_SECURITY_QUESTIONS_OK` or later
  - If MFA required → must be at `FP_MFA_OK`
- New password must match confirmation
- New password must comply with password policy:
  - Minimum/maximum length
  - Uppercase/lowercase requirements
  - Number/symbol requirements
  - Not in password history
  - Not a common password

**Process:**
1. Validates all requirements are met
2. Validates password against policy
3. Updates password hash in correct auth table (CustomerAuth or EmployeeAuth)
4. Sets password expiry based on policy
5. Clears first-time login flags
6. Saves password to history
7. Marks session as completed and revoked
8. Returns success message

## Security Features

### Rate Limiting
All endpoints are rate-limited:
- Identifier: 5 requests per 5 minutes per IP
- Security Questions: 3 requests per 10 minutes per IP
- MFA: 3 requests per 10 minutes per IP
- Password Reset: 3 requests per 10 minutes per IP

### Session Management
- 15-minute session expiry
- Session tied to device ID
- Session revoked after successful password reset
- Session type validation (must be `FORGOT_PASSWORD_TEMP`)

### Privacy Protection
- Does not reveal if identifier exists
- Returns generic error messages
- Logs all attempts for audit

### Password Security
- Passwords hashed with bcrypt
- Password history tracking
- Common password blocking
- Policy-based validation

## Policy Configuration

### Security Question Policy
```sql
-- Enable forgot password security questions for a channel
UPDATE iam_service.security_question_policy 
SET ask_on_forgot_password = true,
    min_questions = 2
WHERE channel = 'INTERNET_BANKING';
```

### MFA Policy
```sql
-- Configure MFA for forgot password
-- MFA is automatically required if MFA policy exists for the channel
UPDATE iam_service.mfa_policy 
SET otp_length = 6,
    otp_expiry_seconds = 120
WHERE channel = 'INTERNET_BANKING';
```

## Implementation Files

### Enums
- `SessionType.java` - Added `FORGOT_PASSWORD_TEMP`
- `LoginStage.java` - Added FP stages

### Domain Models
- `ForgotPasswordRequirements.java` - Requirements model

### Services
- `ForgotPasswordFlowService.java` - Flow management
- `ForgotPasswordIdentifierService.java` - Step 1: Identifier
- `ForgotPasswordSecurityQuestionsService.java` - Step 2: Security questions
- `ForgotPasswordMfaService.java` - Step 3: MFA
- `ForgotPasswordResetService.java` - Step 4: Password reset
- `PasswordUpdateService.java` - Password update utility
- `PolicyEvaluationService.java` - Updated with forgot password requirements evaluation

### Controllers
- `ForgotPasswordController.java` - All endpoints

### DTOs
**Requests:**
- `ForgotPasswordIdentifierRequest.java`
- `ForgotPasswordSecurityQuestionsRequest.java`
- `ForgotPasswordResetRequest.java`

**Responses:**
- `ForgotPasswordIdentifierResponse.java`
- `ForgotPasswordSecurityQuestionsResponse.java`
- `ForgotPasswordResetResponse.java`

## Testing

### Test Scenarios

1. **Forgot Password - No Requirements**
   - Only identifier and new password required
   
2. **Forgot Password - Security Questions Only**
   - Identifier → Security Questions → Password Reset

3. **Forgot Password - MFA Only**
   - Identifier → MFA → Password Reset

4. **Forgot Password - Full Flow**
   - Identifier → Security Questions → MFA → Password Reset

5. **Invalid Identifier**
   - Should return generic error message

6. **Expired Session**
   - Should fail after 15 minutes

7. **Password Policy Violations**
   - Too short, missing requirements, in history, etc.

## Future Enhancements

1. **Email/SMS Notifications**
   - Send notification when forgot password is initiated
   - Send confirmation when password is reset

2. **Temporary Token Method**
   - Alternative flow using secure token sent via email

3. **Account Recovery Questions**
   - Additional verification beyond security questions

4. **IP/Device Tracking**
   - Block suspicious password reset attempts

5. **Admin Approval**
   - Require admin approval for high-risk accounts

## Notes

- Passwords are validated in plain text during forgot password flow (no encryption needed)
- Security questions answers are hashed and case-insensitive
- Session is tied to device ID for additional security
- All attempts are logged for audit trail
- Compatible with existing login flow architecture
