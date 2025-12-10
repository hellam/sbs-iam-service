# Employee Seeder

## Overview
The Employee Seeder creates an initial system administrator employee account in the IAM service. This is useful for bootstrapping a fresh database with a default admin user.

## What It Creates

The seeder creates the following entities in sequence:

1. **Country**: Kenya (KE)
   - Country Code: KE
   - Phone Code: +254
   - Currency: KES (Kenyan Shilling)

2. **Branch**: Headquarters
   - Branch Code: HQ001
   - Branch Name: Headquarters
   - Type: BRANCH
   - City: Nairobi

3. **Party**: Person type party for the employee
   - Party Type: PERSON
   - Status: ACTIVE

4. **Person**: Admin user profile
   - Name: Admin User
   - National ID: 12345678
   - Email: admin@shiva-banking.ke
   - Phone: +254712345678

5. **IAM User**: Identity and access management user
   - Auth Provider: LOCAL
   - Status: ACTIVE

6. **Login Identifier**: Username for authentication
   - Username: **admin**
   - Type: username
   - Channel: BACKOFFICE

7. **Employee Profile**: Employee-specific details
   - Staff No: EMP001
   - Job Title: System Administrator
   - Department: IT
   - Employment Status: ACTIVE

8. **Employee Auth**: Authentication credentials
   - Password: **admin**
   - Algorithm: bcrypt
   - MFA: Disabled

## How to Use

### Enable the Seeder

Add the following configuration to your `application-dev.yaml` or `application.yaml`:

```yaml
seeder:
  employee:
    enabled: true
```

### Run the Application

When the application starts, the seeder will automatically run if enabled. It will:
- Check if the admin user already exists
- Skip execution if the admin user is found
- Create all necessary entities if the admin user doesn't exist

### Default Credentials

After running the seeder, you can log in with:
- **Username**: `admin`
- **Password**: `admin`

**⚠️ IMPORTANT**: Change the default password immediately after first login in production environments!

### Disable the Seeder

To prevent the seeder from running, either:
1. Remove the configuration, or
2. Set it to false:

```yaml
seeder:
  employee:
    enabled: false
```

## Idempotency

The seeder is idempotent and safe to run multiple times:
- It checks for existing data before creating new records
- Country and Branch are checked by their codes
- Admin user is checked by username
- If any data already exists, it reuses the existing records

## Troubleshooting

### Seeder Not Running

Check the following:
1. Verify `seeder.employee.enabled=true` is set in your active profile
2. Check application logs for seeder messages
3. Ensure the database schema is up-to-date (run Liquibase migrations)

### Duplicate Key Errors

If you encounter duplicate key errors:
1. The seeder should handle this automatically
2. Check if partial data was created
3. Consider manually cleaning up the data or adjusting the seeder logic

### Database Connection Issues

Ensure your database configuration is correct in `application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shiva_banking
    username: your_username
    password: your_password
```

## Customization

To customize the seeded data, edit `/config/seeder/EmployeeSeeder.java`:

- Change employee details (name, email, phone)
- Modify branch information
- Update country settings
- Change default credentials (username/password)

## Security Recommendations

1. **Change Default Password**: Immediately change the default `admin` password after first login
2. **Disable in Production**: Only enable the seeder in development environments
3. **Enable MFA**: Enable multi-factor authentication for the admin account
4. **Audit Access**: Monitor login attempts for the admin account

## Files Created

This seeder implementation created the following files:

### Repositories
- `modules/reference/infra/repository/CountryRepository.java`
- `modules/reference/infra/repository/BranchRepository.java`
- `modules/iam/infra/repository/PartyRepository.java`
- `modules/iam/infra/repository/PersonRepository.java`
- `modules/iam/infra/repository/EmployeeProfileRepository.java`

### Seeder
- `config/seeder/EmployeeSeeder.java`

### Configuration
- Updated `LoginIdentifierRepository.java` with `findByIdentifierAndIdentifierType` method
- Updated `application-dev.yaml` with seeder configuration

