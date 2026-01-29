# Password Policy Validation - Implementation Guide

## Overview

The password policy validation for forgot password flow follows the same pattern as the login/password change flow, using custom Jakarta Bean Validation annotations.

## Architecture

### Two Separate Validation Annotations

1. **`@ValidPasswordPolicy`** - Used for password changes during login flow
   - Validator: `PasswordPolicyValidator`
   - Target: `PasswordChangeRequest`
   - Context: Login session (requires old password validation)

2. **`@ValidForgotPasswordPolicy`** - Used for password reset in forgot password flow
   - Validator: `ForgotPasswordPolicyValidator`
   - Target: `ForgotPasswordResetRequest`
   - Context: Forgot password session (no old password needed)

## Why Separate Annotations?

### Different Flow Contexts
- **Login Flow**: Uses `LoginFlowService` with `SessionType.LOGIN_TEMP`
- **Forgot Password Flow**: Uses `ForgotPasswordFlowService` with `SessionType.FORGOT_PASSWORD_TEMP`

### Different Validation Requirements
- **Password Change**: Validates old password + new password
- **Password Reset**: Only validates new password (no old password)

### Different Stage Requirements
- **Password Change**: Requires `LoginStage.IDENTIFIER_OK` or later
- **Password Reset**: Requires `LoginStage.FP_IDENTIFIER_OK` or later

## Implementation Details

### @ValidForgotPasswordPolicy Annotation

```java
@Documented
@Constraint(validatedBy = ForgotPasswordPolicyValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidForgotPasswordPolicy {
    String message() default "Invalid password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

### ForgotPasswordPolicyValidator

```java
@Component
public class ForgotPasswordPolicyValidator 
    implements ConstraintValidator<ValidForgotPasswordPolicy, ForgotPasswordResetRequest> {
    
    @Override
    public boolean isValid(ForgotPasswordResetRequest request, 
                          ConstraintValidatorContext context) {
        // 1. Get flowId from FlowIdProvider
        // 2. Load forgot password session
        // 3. Get password policy for channel
        // 4. Validate structure (length, complexity)
        // 5. Validate against history
        // 6. Validate against common passwords
        // 7. Return true/false with custom error messages
    }
}
```

## Password Policy Validations

Both validators check the same password policy rules:

### 1. Structure Validation
- Minimum length
- Maximum length
- Require uppercase
- Require lowercase
- Require number
- Require symbol

### 2. History Validation
- Prevents reusing last N passwords
- Configurable via `passwordHistoryCount` in policy

### 3. Common Passwords
- Blocks commonly used passwords
- Configurable via `blockCommonPasswords` in policy

## Usage Example

### ForgotPasswordResetRequest

```java
@Data
@ValidForgotPasswordPolicy(message = "Password does not meet the policy requirements.")
public class ForgotPasswordResetRequest {
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @AssertTrue(message = "Password confirmation does not match")
    private boolean isPasswordConfirmed() {
        return Objects.equals(this.newPassword, this.confirmPassword);
    }
}
```

### Controller Endpoint

```java
@PostMapping("/reset")
public ResponseEntity<ApiResponse<ForgotPasswordResetResponse>> resetPassword(
    @Valid @RequestBody ForgotPasswordResetRequest req,  // @Valid triggers validation
    @FlowId UUID flowId
) {
    return ResponseBuilder.success(resetService.handle(req, flowId));
}
```

## Validation Flow

```
1. HTTP Request arrives
   ↓
2. Spring validates @Valid request body
   ↓
3. @NotBlank, @Size validations run
   ↓
4. @AssertTrue password confirmation check runs
   ↓
5. @ValidForgotPasswordPolicy annotation validator runs
   ↓
6. Validator gets flowId from FlowIdProvider
   ↓
7. Validator loads session from ForgotPasswordFlowService
   ↓
8. Validator gets password policy for channel
   ↓
9. Validator checks structure, history, common passwords
   ↓
10. If validation fails → ConstraintViolationException
11. If validation passes → Controller method executes
```

## Error Handling

### Validation Errors Return 400 Bad Request

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "newPassword": "Password must contain an uppercase letter"
  }
}
```

### Custom Error Messages

The validator can provide context-specific error messages:
- "Password is too short (min 8)"
- "Password must contain an uppercase letter"
- "Password cannot match any of the last 5 used passwords"
- "Password is too common and easily guessable"

## Benefits of This Approach

### 1. Declarative Validation
- Clean, annotation-based validation
- No manual validation code in service layer
- Separation of concerns

### 2. Consistent Error Handling
- Standard Jakarta Bean Validation errors
- Automatic conversion to API error format
- Consistent error structure across all endpoints

### 3. Reusable Components
- `PasswordPolicyService` methods are reused
- FlowIdProvider pattern is consistent
- Easy to test and maintain

### 4. Early Validation
- Validation happens before controller method
- Fails fast if password doesn't meet policy
- Reduces unnecessary processing

### 5. Type Safety
- Strongly typed request objects
- Compile-time checking
- IDE support for validation rules

## Testing

### Unit Test Example

```java
@Test
void testPasswordTooShort() {
    ForgotPasswordResetRequest request = new ForgotPasswordResetRequest();
    request.setNewPassword("Short1!");
    request.setConfirmPassword("Short1!");
    
    Set<ConstraintViolation<ForgotPasswordResetRequest>> violations = 
        validator.validate(request);
    
    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> 
        v.getMessage().contains("Password is too short"));
}
```

### Integration Test Example

```java
@Test
void testForgotPasswordReset_InvalidPassword() {
    mockMvc.perform(post("/api/v1/forgot-password/reset")
        .header("X-Flow-ID", flowId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
                "newPassword": "short",
                "confirmPassword": "short"
            }
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.newPassword").exists());
}
```

## Configuration

### Password Policy Table

```sql
-- Configure password policy
UPDATE iam_service.password_policy 
SET min_length = 8,
    max_length = 128,
    require_uppercase = true,
    require_lowercase = true,
    require_number = true,
    require_symbol = true,
    password_history_count = 5,
    block_common_passwords = true
WHERE channel = 'INTERNET_BANKING';
```

## Files Modified

1. **Created**: `ValidForgotPasswordPolicy.java` - Annotation
2. **Created**: `ForgotPasswordPolicyValidator.java` - Validator implementation
3. **Updated**: `ForgotPasswordResetRequest.java` - Added annotation and confirmation check
4. **Updated**: `ForgotPasswordResetService.java` - Removed manual validation (now handled by annotation)

## Comparison with Reusing ValidPasswordPolicy

### Why NOT Reuse ValidPasswordPolicy?

| Aspect | Separate Annotation | Reused Annotation |
|--------|-------------------|-------------------|
| **Flow Context** | ✅ Correct flow service | ❌ Wrong flow service |
| **Session Type** | ✅ FORGOT_PASSWORD_TEMP | ❌ LOGIN_TEMP |
| **Old Password** | ✅ Not required | ❌ Would try to validate |
| **Stage Validation** | ✅ FP_IDENTIFIER_OK+ | ❌ IDENTIFIER_OK+ |
| **Maintainability** | ✅ Clear separation | ❌ Mixed concerns |
| **Testing** | ✅ Independent tests | ❌ Coupled tests |

## Conclusion

Creating a separate `@ValidForgotPasswordPolicy` annotation provides:
- Clear separation of concerns
- Correct flow context handling
- Better maintainability
- Independent testing
- Type safety

While reusing `@ValidPasswordPolicy` might seem DRY, the different flow contexts and validation requirements justify the separation. Both validators share the same underlying `PasswordPolicyService` methods, so there's no code duplication at the business logic level.
