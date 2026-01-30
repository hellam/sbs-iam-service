# Forgot Password Flow - Quick Reference

## Flow Overview
```
1. POST /api/v1/forgot-password/identifier/{channel}
   ↓
2. [Optional] POST /api/v1/forgot-password/security-questions/verify
   ↓
3. [Optional] POST /api/v1/forgot-password/mfa/initiate
   ↓
4. [Optional] POST /api/v1/forgot-password/mfa/verify
   ↓
5. POST /api/v1/forgot-password/reset
```

## Quick Start

### 1. Initiate Forgot Password
```bash
curl -X POST http://localhost:9001/api/v1/forgot-password/identifier/internet-banking \
  -H "Content-Type: application/json" \
  -H "X-Device-ID: your-device-id" \
  -d '{
    "identifier": "user@example.com"
  }'
```

Response tells you what steps are required:
```json
{
  "flowId": "uuid-here",
  "securityQuestionsRequired": true,
  "securityQuestionsCount": 2,
  "mfaRequired": true,
  "nextStep": "SECURITY_QUESTIONS"
}
```

### 2. If Security Questions Required
```bash
curl -X POST http://localhost:9001/api/v1/forgot-password/security-questions/verify \
  -H "Content-Type: application/json" \
  -H "X-Flow-ID: uuid-here" \
  -d '{
    "flowId": "uuid-here",
    "answers": [
      {"questionId": 1, "answer": "Paris"},
      {"questionId": 3, "answer": "Fluffy"}
    ]
  }'
```

### 3. If MFA Required
```bash
# Initiate MFA
curl -X POST http://localhost:9001/api/v1/forgot-password/mfa/initiate \
  -H "Content-Type: application/json" \
  -H "X-Flow-ID: uuid-here" \
  -d '{"channel": "SMS"}'

# Verify MFA
curl -X POST http://localhost:9001/api/v1/forgot-password/mfa/verify \
  -H "Content-Type: application/json" \
  -H "X-Flow-ID: uuid-here" \
  -d '{"otp": "123456"}'
```

### 4. Reset Password
```bash
curl -X POST http://localhost:9001/api/v1/forgot-password/reset \
  -H "Content-Type: application/json" \
  -H "X-Flow-ID: uuid-here" \
  -d '{
    "newPassword": "MyNewP@ssw0rd",
    "confirmPassword": "MyNewP@ssw0rd"
  }'
```

## Policy Configuration

### Enable Security Questions for Forgot Password
```sql
UPDATE iam_service.security_question_policy 
SET ask_on_forgot_password = true,
    enabled = true,
    min_questions = 2
WHERE channel = 'INTERNET_BANKING';
```

### MFA Configuration
MFA is automatically used if MFA policy exists for the channel:
```sql
-- MFA already configured via existing mfa_policy table
SELECT * FROM iam_service.mfa_policy WHERE channel = 'INTERNET_BANKING';
```

## Flow Stages

| Stage | Description |
|-------|-------------|
| `FP_IDENTIFIER_OK` | User identifier verified |
| `FP_SECURITY_QUESTIONS_OK` | Security questions answered |
| `FP_MFA_OK` | MFA verification completed |
| `FP_PASSWORD_RESET` | Password successfully reset |

## Common Scenarios

### Scenario 1: No Extra Verification
User → Identifier → Reset Password

### Scenario 2: Security Questions Only
User → Identifier → Security Questions → Reset Password

### Scenario 3: MFA Only
User → Identifier → MFA → Reset Password

### Scenario 4: Full Verification
User → Identifier → Security Questions → MFA → Reset Password

## Important Notes

1. **Session Expiry**: 15 minutes
2. **Rate Limiting**: Active on all endpoints
3. **Device ID**: Required in X-Device-ID header
4. **Flow ID**: Returned in step 1, used in subsequent steps via X-Flow-ID header
5. **Plain Text Passwords**: Unlike login flow, forgot password accepts plain text passwords (no RSA encryption)
6. **Generic Errors**: Invalid identifiers return generic error for security

## Testing Tips

1. Create test user with known identifier
2. Set up security questions if policy requires them
3. Ensure user has verified contact for MFA
4. Test password policy validations (length, complexity, history)
5. Test session expiry after 15 minutes
6. Test rate limiting

## Key Services

- `ForgotPasswordFlowService` - Manages session/flow
- `ForgotPasswordIdentifierService` - Validates identifier
- `ForgotPasswordSecurityQuestionsService` - Verifies security questions
- `ForgotPasswordMfaService` - Handles MFA
- `ForgotPasswordResetService` - Updates password
- `PasswordUpdateService` - Updates password in auth tables
- `PolicyEvaluationService` - Determines requirements

## Error Handling

All errors return standard API error format:
```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2026-01-29T..."
}
```

Common errors:
- Invalid identifier: "If an account with this identifier exists..."
- Session expired: "Page expired. Please refresh and try again."
- Invalid stage: "Invalid flow stage"
- Password mismatch: "Passwords do not match"
- Policy violation: Specific message about requirement
