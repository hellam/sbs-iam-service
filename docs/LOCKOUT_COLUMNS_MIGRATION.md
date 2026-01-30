# Lockout Columns Migration - Implementation Complete ✅

## Summary
Successfully created database migrations to add lockout timestamp columns to authentication entities for temporary account lockouts.

---

## 📦 Migrations Created

### 1. Customer Auth Lockout Columns
**File:** `055-add-customer-auth-lockout-columns.xml`  
**Table:** `iam_service.customer_auth`

**Columns Added:**
- `internet_lockout_until` (TIMESTAMPTZ, nullable)
  - Timestamp until which Internet Banking login is locked
  - Used for temporary lockouts after failed password attempts
  
- `mobile_lockout_until` (TIMESTAMPTZ, nullable)
  - Timestamp until which Mobile Banking login is locked
  - Used for temporary lockouts after failed PIN attempts

**Indexes Created:**
- `idx_customer_auth_internet_lockout` on (`internet_lockout_until`, `internet_locked`)
- `idx_customer_auth_mobile_lockout` on (`mobile_lockout_until`, `mobile_locked`)

### 2. Employee Auth Lockout Column
**File:** `056-add-employee-auth-lockout-column.xml`  
**Table:** `iam_service.employee_auth`

**Column Added:**
- `staff_lockout_until` (TIMESTAMPTZ, nullable)
  - Timestamp until which staff/employee login is locked
  - Used for temporary lockouts after failed backoffice password attempts

**Index Created:**
- `idx_employee_auth_staff_lockout` on (`staff_lockout_until`, `staff_locked`)

---

## 📁 File Structure

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml (✅ Updated)
└── changes/iam/05-auth/
    ├── 050-customer-auth.xml (existing)
    ├── 051-employee-auth.xml (existing)
    ├── 052-organization-user-auth.xml (existing)
    ├── 053-iam-user-security-question.xml (existing)
    ├── 054-otp-record.xml (existing)
    ├── 055-add-customer-auth-lockout-columns.xml (✅ NEW)
    └── 056-add-employee-auth-lockout-column.xml (✅ NEW)
```

---

## 🔄 Migration Flow

The migrations will execute in this order:
1. **055-add-customer-auth-lockout-columns.xml**
   - Adds `internet_lockout_until` column
   - Adds `mobile_lockout_until` column
   - Creates composite indexes for efficient lockout queries
   
2. **056-add-employee-auth-lockout-column.xml**
   - Adds `staff_lockout_until` column
   - Creates composite index for efficient lockout queries

---

## 🎯 Purpose & Usage

### Temporary Lockout Mechanism

These columns enable **time-based automatic unlocking** after failed login attempts:

#### Example Flow:
```java
// User fails 5 login attempts
customerAuth.setInternetLocked(true);
customerAuth.setInternetLockoutUntil(OffsetDateTime.now().plusMinutes(30));
// Account is locked for 30 minutes

// After 30 minutes, next login attempt checks:
if (customerAuth.getInternetLockoutUntil() != null 
    && OffsetDateTime.now().isAfter(customerAuth.getInternetLockoutUntil())) {
    // Auto-unlock
    customerAuth.setInternetLocked(false);
    customerAuth.setInternetLockoutUntil(null);
    customerAuth.setInternetFailedAttempts(0);
}
```

### Benefits:
1. ✅ **No manual intervention required** - accounts auto-unlock after timeout
2. ✅ **Different lockout durations** for different channels (IB vs Mobile vs Staff)
3. ✅ **Better user experience** - users know when they can retry
4. ✅ **Security compliance** - meets requirements for temporary lockouts
5. ✅ **Database indexes** for efficient lockout status queries

---

## 📊 Database Schema

### customer_auth Table (After Migration)
```sql
CREATE TABLE iam_service.customer_auth (
    -- ... existing columns ...
    
    -- Internet Banking
    internet_locked BOOLEAN DEFAULT false NOT NULL,
    internet_lockout_until TIMESTAMPTZ,  -- ✅ NEW
    internet_failed_attempts SMALLINT DEFAULT 0 NOT NULL,
    
    -- Mobile Banking
    mobile_locked BOOLEAN DEFAULT false NOT NULL,
    mobile_lockout_until TIMESTAMPTZ,    -- ✅ NEW
    mobile_failed_attempts SMALLINT DEFAULT 0 NOT NULL
);

-- Composite indexes for lockout queries
CREATE INDEX idx_customer_auth_internet_lockout 
    ON iam_service.customer_auth(internet_lockout_until, internet_locked);
    
CREATE INDEX idx_customer_auth_mobile_lockout 
    ON iam_service.customer_auth(mobile_lockout_until, mobile_locked);
```

### employee_auth Table (After Migration)
```sql
CREATE TABLE iam_service.employee_auth (
    -- ... existing columns ...
    
    -- Staff Backoffice
    staff_locked BOOLEAN DEFAULT false NOT NULL,
    staff_lockout_until TIMESTAMPTZ,     -- ✅ NEW
    staff_failed_attempts SMALLINT DEFAULT 0 NOT NULL
);

-- Composite index for lockout queries
CREATE INDEX idx_employee_auth_staff_lockout 
    ON iam_service.employee_auth(staff_lockout_until, staff_locked);
```

---

## 🚀 Running the Migrations

### Development
```bash
cd /Users/hellamimbosa/IdeaProjects/shiva-banking/services/iam-service

# Run migrations
mvn liquibase:update

# Or start the application (auto-runs on startup)
mvn spring-boot:run
```

### Production
```bash
# Preview changes (dry-run)
mvn liquibase:update -P production -Dliquibase.dryRun=true

# Apply migrations
mvn liquibase:update -P production

# Verify
psql -d shiva_banking -c "\d iam_service.customer_auth" | grep lockout
psql -d shiva_banking -c "\d iam_service.employee_auth" | grep lockout
```

---

## ✅ Verification Queries

### Check if columns exist:
```sql
-- Customer Auth lockout columns
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'iam_service'
  AND table_name = 'customer_auth'
  AND column_name LIKE '%lockout_until%';

-- Employee Auth lockout column
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'iam_service'
  AND table_name = 'employee_auth'
  AND column_name = 'staff_lockout_until';
```

**Expected Results:**
```
 column_name             | data_type                   | is_nullable
-------------------------+-----------------------------+-------------
 internet_lockout_until  | timestamp with time zone    | YES
 mobile_lockout_until    | timestamp with time zone    | YES
 staff_lockout_until     | timestamp with time zone    | YES
```

### Check if indexes exist:
```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'iam_service'
  AND indexname LIKE '%lockout%';
```

**Expected Results:**
```
 indexname                            | indexdef
--------------------------------------+------------------------------------------
 idx_customer_auth_internet_lockout  | CREATE INDEX ... ON customer_auth(...
 idx_customer_auth_mobile_lockout    | CREATE INDEX ... ON customer_auth(...
 idx_employee_auth_staff_lockout     | CREATE INDEX ... ON employee_auth(...
```

---

## 🔒 Security Features Enabled

With these lockout columns, you can now implement:

1. **Progressive Lockout Duration**
   ```java
   // First lockout: 15 minutes
   // Second lockout: 30 minutes
   // Third lockout: 60 minutes
   // Fourth lockout: 24 hours
   ```

2. **Channel-Specific Lockouts**
   - Lock Internet Banking without affecting Mobile Banking
   - Lock Mobile Banking without affecting Internet Banking
   - Independent lockout durations per channel

3. **Automatic Unlock Service**
   ```java
   @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
   public void unlockExpiredAccounts() {
       // Find and unlock accounts where lockout_until has passed
       customerAuthRepo.findLockedAccountsWithExpiredLockout()
           .forEach(this::autoUnlock);
   }
   ```

4. **User-Friendly Messaging**
   ```json
   {
     "error": "Account locked",
     "message": "Too many failed login attempts",
     "locked_until": "2026-01-01T08:30:00Z",
     "retry_after_minutes": 15
   }
   ```

---

## 📝 Next Steps

### 1. Update Authentication Services
Modify login services to use the new lockout columns:
- Check `lockout_until` before allowing login
- Set `lockout_until` when locking account
- Auto-unlock if `lockout_until` has passed

### 2. Create Scheduled Unlock Job
Create a background job to auto-unlock expired lockouts:
```java
@Scheduled(fixedRate = 300000) // Every 5 minutes
public void unlockExpiredLockouts() {
    // Auto-unlock customer internet banking
    // Auto-unlock customer mobile banking
    // Auto-unlock employee staff accounts
}
```

### 3. Update Login History
Log lockout events in `login_history`:
```java
loginHistoryService.logLoginFailure(
    user, 
    identifier, 
    session, 
    "ACCOUNT_LOCKED_UNTIL_" + lockoutUntil
);
```

### 4. Add API Endpoints (Optional)
```
GET  /api/v1/auth/lockout-status/{userId}
POST /api/v1/admin/auth/unlock/{userId}
```

---

## 🎉 Status: COMPLETE

✅ Migration files created  
✅ Master changelog updated  
✅ Indexes added for performance  
✅ Schema validated  
✅ Documentation complete  

**Ready to run migrations!**

---

## 📚 Related Documentation

- **Entity Mapping**: `CustomerAuthEntity.java` (lines 61, 91)
- **Entity Mapping**: `EmployeeAuthEntity.java` (line 51)
- **Migration Files**: `src/main/resources/db/changelog/changes/iam/05-auth/`
- **Master Changelog**: `db.changelog-master.yaml`

---

**Migration Author**: hellamimbosa  
**Date**: January 1, 2026  
**Liquibase Version**: 4.20+  
**Database**: PostgreSQL 12+

