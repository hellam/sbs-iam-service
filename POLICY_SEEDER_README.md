# Policy Seeder

## Overview
The Policy Seeder creates initial security policies for PIN, Password, MFA, and Security Questions across all channels (Mobile Banking, Internet Banking, Backoffice). It also creates sample features and a global feature policy.

## What It Creates

### Security Policies
For each channel (MOBILE_BANKING, INTERNET_BANKING, BACKOFFICE), the seeder creates:

1. **PIN Policy**
   - PolicyEntity with type PIN_POLICY
   - PinPolicyEntity with default settings (min 4, max 6, etc.)

2. **Password Policy**
   - PolicyEntity with type PASSWORD_POLICY
   - PasswordPolicyEntity with default settings (min 12, max 128, etc.)

3. **MFA Policy**
   - PolicyEntity with type MFA_POLICY
   - MfaPolicyEntity with default settings (MFA enabled for IB and Backoffice, etc.)

4. **Security Question Policy**
   - PolicyEntity with type SEC_QN_POLICY
   - SecurityQuestionPolicyEntity with default settings (disabled by default)

### Features and Feature Policy
1. **FeatureEntity**: LOGIN and TRANSFER features
2. **FeaturePolicyEntity**: Global policy containing the two features

## How to Use

### Enable the Seeder

Add the following configuration to your `application-dev.yaml`:

```yaml
seeder:
  policy:
    enabled: true
```

### Run the Application

When the application starts, the seeder will automatically run if enabled. It will:
- Create policies for each channel and type
- Create sample features if they don't exist
- Create a global feature policy with the features

### Disable the Seeder

To prevent the seeder from running, set it to false:

```yaml
seeder:
  policy:
    enabled: false
```

## Idempotency

The seeder is idempotent:
- Policies are created only if they don't exist (based on name and channel)
- Features are checked by code
- Global feature policy is checked by name, channel, and scope

## Default Policy Settings

### PIN Policy
- Min Length: 4
- Max Length: 6
- History Count: 5
- Block Sequential: true
- Block Repeating: true
- Max Failed Attempts: 5
- Lockout Minutes: 30
- Hash Algorithm: bcrypt
- Hash Cost: 10

### Password Policy
- Min Length: 12
- Max Length: 128
- Require Uppercase: false
- Require Lowercase: false
- Require Number: false
- Require Symbol: false
- Block Common Passwords: true
- History Count: 5
- Expiration Enabled: false
- Expiration Days: 90
- Max Failed Attempts: 5
- Lockout Minutes: 30
- Require Factory Reset: false
- Hash Algorithm: bcrypt
- Hash Cost: 12

### MFA Policy
- Require MFA IB: true
- Require MFA MB: false
- Require MFA Backoffice: true
- Allow TOTP: true
- Allow SMS OTP: true
- Allow Email OTP: true
- Allow WhatsApp OTP: true
- Allow Push: false
- Allow WebAuthn: false
- OTP Expiry Seconds: 120
- OTP Daily Limit: 10
- Require MFA High Value Txn: true
- High Value Threshold: 5000
- Enforce on New Device: true
- Enforce on New Location: true

### Security Question Policy
- Enabled: false
- Min Questions: 0
- Max Questions: 0
- Mandatory: false
- Ask on Forgot Password: false
- Ask on Sensitive Action: false
- Is Active: true

## Features Created
- **LOGIN**: User Login (Authentication category)
- **TRANSFER**: Fund Transfer (Transactions category)

## Global Feature Policy
- Name: "Global Feature Policy"
- Contains LOGIN and TRANSFER features
- Scope: GLOBAL
- Channel: BACKOFFICE (default for global)
- Active: true
