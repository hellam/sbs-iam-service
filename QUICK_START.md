# Quick Start Guide - Employee Seeder

## ✅ What Was Created

I've successfully created an **Employee Seeder** for your IAM service that automatically creates a system administrator account when the application starts.

## 📦 Files Created

### Repositories (5 new files)
1. `CountryRepository.java` - Manages country data
2. `BranchRepository.java` - Manages branch data
3. `PartyRepository.java` - Manages party entities
4. `PersonRepository.java` - Manages person profiles
5. `EmployeeProfileRepository.java` - Manages employee profiles

### Seeder
6. `EmployeeSeeder.java` - Main seeder that creates the admin employee

### Updated Files
7. `LoginIdentifierRepository.java` - Added lookup method
8. `application-dev.yaml` - Added seeder configuration

## 🚀 How to Use

### Step 1: The seeder is already enabled in development
The configuration has been added to `application-dev.yaml`:
```yaml
seeder:
  employee:
    enabled: true
```

### Step 2: Run the application
```bash
cd /Users/hellamimbosa/IdeaProjects/shiva-banking/services/iam-service
./mvnw spring-boot:run
```

### Step 3: Watch the logs
You should see:
```
Starting employee seeder...
Creating country: Kenya
Creating branch: Headquarters
Creating party for employee
Creating person: Admin User
Creating IAM user
Creating login identifier: admin
Creating employee profile
Creating employee auth with password: admin
Employee seeder completed successfully!
Created employee with username: admin, password: admin
Staff No: EMP001
Branch: Headquarters (HQ001)
```

### Step 4: Login
Use these credentials:
- **Username**: `admin`
- **Password**: `admin`
- **Channel**: BACKOFFICE

## 📝 What Gets Created

The seeder creates a complete employee record:

| Entity | Details |
|--------|---------|
| **Country** | Kenya (KE), +254, KES |
| **Branch** | HQ001 - Headquarters, Nairobi |
| **Party** | Type: PERSON, Status: ACTIVE |
| **Person** | Admin User, admin@shiva-banking.ke |
| **IAM User** | Auth: LOCAL, Status: ACTIVE |
| **Login** | Username: admin, Channel: BACKOFFICE |
| **Employee** | EMP001, System Administrator, IT |
| **Auth** | Password: admin (bcrypt) |

## 🔒 Security Features

- ✅ Password is hashed using bcrypt
- ✅ Seeder only runs if enabled in config
- ✅ Skips if admin user already exists
- ✅ Idempotent - safe to run multiple times
- ⚠️ **Remember to change the default password!**

## 🛠 Control the Seeder

### Disable for production
Edit `application-prod.yaml`:
```yaml
seeder:
  employee:
    enabled: false
```

### Disable for all environments
Remove or set to false in `application-dev.yaml`:
```yaml
seeder:
  employee:
    enabled: false
```

## ✨ Smart Features

1. **Checks if data exists** - Won't create duplicates
2. **Reuses existing data** - If Kenya/HQ001 exist, uses them
3. **Comprehensive logging** - See exactly what's happening
4. **Error handling** - Fails gracefully with clear messages
5. **Transaction support** - All-or-nothing creation

## 🧪 Testing

After the application starts:

1. Check the database:
```sql
-- Check if admin was created
SELECT * FROM iam_service.login_identifier WHERE identifier = 'admin';

-- Check employee profile
SELECT * FROM iam_service.employee_profile WHERE staff_no = 'EMP001';

-- Check person details
SELECT * FROM iam_service.person WHERE email = 'admin@shiva-banking.ke';
```

2. Try logging in with `admin/admin` via your authentication endpoint

## 📚 Documentation

- `SEEDER_README.md` - Full documentation
- `SEEDER_SUMMARY.md` - Implementation summary
- `QUICK_START.md` - This file

## 🎯 Next Steps

1. ✅ Start the application
2. ✅ Verify the seeder runs successfully
3. ✅ Login with admin/admin
4. ⚠️ Change the default password
5. 🔧 Configure additional roles/permissions
6. 🚫 Disable seeder in production

## ❓ Troubleshooting

**Seeder not running?**
- Check `seeder.employee.enabled=true` in application-dev.yaml
- Verify database connection
- Check liquibase migrations ran

**Already exists?**
- The seeder will skip if admin user exists
- Check logs: "Admin user already exists. Skipping seeder."

**Database errors?**
- Ensure schema migrations have run
- Check database credentials in application.yaml

---

**Status**: ✅ Ready to use!

**Default Credentials**: 
- Username: `admin`
- Password: `admin`
- Change after first login!

