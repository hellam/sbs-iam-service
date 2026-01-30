# Security Challenge Attempt Repository - Implementation Summary

## Created

### SecurityChallengeAttemptRepository.java
**Location**: `infra/repository/`

**Purpose**: Repository for tracking security question challenge attempts during forgot password and other authentication flows.

**Entity**: `SecurityChallengeAttemptEntity`

**Key Methods**:

1. **Basic CRUD**
   - `save()` - inherited from JpaRepository
   - `findById()` - inherited from JpaRepository

2. **Query Methods**
   ```java
   // Find all attempts by user
   List<SecurityChallengeAttemptEntity> findByIamUser(IamUserEntity iamUser)
   
   // Find recent attempts by user
   List<SecurityChallengeAttemptEntity> findByIamUserAndCreatedAtAfter(
       IamUserEntity iamUser, 
       OffsetDateTime after
   )
   
   // Count failed attempts (for lockout logic)
   long countFailedAttemptsByUserAfter(
       IamUserEntity user, 
       OffsetDateTime after
   )
   
   // Count attempts by user and IP (for rate limiting)
   long countAttemptsByUserAndIpAfter(
       IamUserEntity user,
       String ipAddress,
       OffsetDateTime after
   )
   
   // Find attempts by device
   List<SecurityChallengeAttemptEntity> findByDeviceIdAndCreatedAtAfter(
       String deviceId, 
       OffsetDateTime after
   )
   ```

## Entity Structure

```java
SecurityChallengeAttemptEntity:
├── id (Long) - Primary key
├── iamUser (IamUserEntity) - User who attempted
├── securityQuestion (SecurityQuestionEntity) - Question asked
├── answerCorrect (Boolean) - Whether answer was correct
├── ipAddress (String) - IP address of attempt
├── deviceId (String) - Device ID of attempt
├── channel (Channel) - Channel (INTERNET_BANKING, MOBILE, etc.)
└── createdAt (OffsetDateTime) - When attempt was made
```

## Usage in ForgotPasswordSecurityQuestionsService

### Before
```java
// No attempt tracking
for (IamUserSecurityQuestionEntity userQuestion : userQuestions) {
    // Verify answer
    // No logging
}
```

### After
```java
// Track each attempt
for (IamUserSecurityQuestionEntity userQuestion : userQuestions) {
    boolean isCorrect = verifyAnswer(providedAnswer, userQuestion);
    
    // Log the attempt
    SecurityChallengeAttemptEntity attempt = new SecurityChallengeAttemptEntity();
    attempt.setIamUser(user);
    attempt.setSecurityQuestion(userQuestion.getSecurityQuestion());
    attempt.setAnswerCorrect(isCorrect); // Track success/failure
    attempt.setChannel(session.getChannel());
    attempt.setCreatedAt(OffsetDateTime.now());
    
    // Extract request context
    RequestContext context = requestContextExtractor.extractContext();
    if (context != null) {
        attempt.setDeviceId(context.getDeviceId());
        attempt.setIpAddress(context.getIpAddress());
    }
    
    securityChallengeAttemptRepository.save(attempt);
}
```

## Benefits

1. **Audit Trail** - Track all security question attempts
2. **Security Analytics** - Identify suspicious patterns
3. **Account Lockout** - Count failed attempts for lockout policy
4. **Rate Limiting** - Prevent brute force attacks by IP/device
5. **Compliance** - Meet regulatory requirements for authentication logging

## Future Use Cases

### 1. Account Lockout Policy
```java
// Count recent failed attempts
long failedAttempts = repository.countFailedAttemptsByUserAfter(
    user, 
    OffsetDateTime.now().minusMinutes(30)
);

if (failedAttempts >= 5) {
    // Lock account
}
```

### 2. IP-Based Rate Limiting
```java
// Check attempts from same IP
long ipAttempts = repository.countAttemptsByUserAndIpAfter(
    user,
    ipAddress,
    OffsetDateTime.now().minusHours(1)
);

if (ipAttempts >= 10) {
    // Block IP temporarily
}
```

### 3. Device Tracking
```java
// Find suspicious device activity
List<SecurityChallengeAttemptEntity> deviceAttempts = 
    repository.findByDeviceIdAndCreatedAtAfter(
        deviceId,
        OffsetDateTime.now().minusDays(7)
    );

if (deviceAttempts.size() > threshold) {
    // Flag for review
}
```

### 4. Security Analytics
```java
// Analyze patterns
List<SecurityChallengeAttemptEntity> recentAttempts = 
    repository.findByIamUserAndCreatedAtAfter(
        user,
        OffsetDateTime.now().minusDays(30)
    );

// Generate security report
```

## Database Table

**Table**: `security_challenge_attempt`

**Schema**: `iam_service`

**Columns**:
- `id` - BIGINT PRIMARY KEY
- `iam_user_id` - BIGINT NOT NULL (FK to iam_user)
- `security_question_id` - BIGINT NOT NULL (FK to security_question)
- `answer_correct` - BOOLEAN DEFAULT false
- `ip_address` - VARCHAR(64)
- `device_id` - VARCHAR(255)
- `channel` - VARCHAR(50)
- `created_at` - TIMESTAMP DEFAULT now()

**Indexes** (recommended):
```sql
CREATE INDEX idx_sca_user_created ON security_challenge_attempt(iam_user_id, created_at);
CREATE INDEX idx_sca_ip_created ON security_challenge_attempt(ip_address, created_at);
CREATE INDEX idx_sca_device_created ON security_challenge_attempt(device_id, created_at);
```

## Testing

### Unit Test Example
```java
@Test
void testSaveAttempt() {
    SecurityChallengeAttemptEntity attempt = new SecurityChallengeAttemptEntity();
    attempt.setIamUser(user);
    attempt.setSecurityQuestion(question);
    attempt.setAnswerCorrect(true);
    attempt.setChannel(Channel.INTERNET_BANKING);
    
    SecurityChallengeAttemptEntity saved = repository.save(attempt);
    
    assertNotNull(saved.getId());
}

@Test
void testCountFailedAttempts() {
    // Create 3 failed attempts
    createFailedAttempts(3);
    
    long count = repository.countFailedAttemptsByUserAfter(
        user,
        OffsetDateTime.now().minusHours(1)
    );
    
    assertEquals(3, count);
}
```

## Security Considerations

1. **PII Protection** - IP addresses are logged but should be handled per privacy policy
2. **Data Retention** - Consider cleanup policy for old attempts
3. **Index Performance** - Ensure queries on created_at are fast
4. **Rate Limiting** - Use this data for rate limiting decisions

## Status

✅ Repository created
✅ Methods defined for common queries
✅ Integration with ForgotPasswordSecurityQuestionsService complete
✅ Attempt tracking implemented
✅ Context extraction (IP, device) working
✅ All code compiles successfully

## Files Modified

1. **Created**: `SecurityChallengeAttemptRepository.java`
2. **Modified**: `ForgotPasswordSecurityQuestionsService.java`
   - Added repository dependency
   - Implemented attempt logging for each question
   - Track correct/incorrect answers
   - Extract and store IP and device context
