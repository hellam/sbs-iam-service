# Policy Seeder

## Overview
The Policy Seeder creates initial security policies for PIN, Password, MFA, and Security Questions across all channels (Mobile Banking, Internet Banking, Backoffice). It also creates sample features and a global feature policy.

## What It Creates

### Security Policies
The seeder creates policies with channel-specific rules:

1. **PIN Policy** - Only for MOBILE_BANKING and INTERNET_BANKING
   - PolicyEntity with type PIN_POLICY
   - PinPolicyEntity with default settings (min 4, max 6, etc.) for each channel

2. **Password Policy** - Only for INTERNET_BANKING and BACKOFFICE
   - PolicyEntity with type PASSWORD_POLICY
   - PasswordPolicyEntity with default settings (min 12, max 128, etc.) for each channel

3. **MFA Policy** - For all channels (MOBILE_BANKING, INTERNET_BANKING, BACKOFFICE)
   - PolicyEntity with type MFA_POLICY
   - MfaPolicyEntity with default settings (MFA enabled for IB and Backoffice, etc.) for each channel

4. **Security Question Policy** - For all channels (MOBILE_BANKING, INTERNET_BANKING, BACKOFFICE)
   - PolicyEntity with type SEC_QN_POLICY
   - SecurityQuestionPolicyEntity with default settings (disabled by default) for each channel

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
- Contains feature IDs for LOGIN and TRANSFER (stored as Set<Long>)
- Scope: GLOBAL
- Channel: BACKOFFICE (default for global)
- Active: true

## Important Implementation Details

### Policy Type Consolidation
The seeder ensures **one policy row per PolicyType** by:
1. Checking if any policy of the given type already contains the channel
2. If found, skipping (policy already configured for that channel)
3. If not found but a policy of that type exists, adding the channel to the existing policy's channels array
4. If no policy of that type exists, creating a new policy with the channel

This means after running the seeder, you will have:
- 1 PIN_POLICY row with channels: [MOBILE_BANKING, INTERNET_BANKING]
- 1 PASSWORD_POLICY row with channels: [INTERNET_BANKING, BACKOFFICE]
- 1 MFA_POLICY row with channels: [MOBILE_BANKING, INTERNET_BANKING, BACKOFFICE]
- 1 SEC_QN_POLICY row with channels: [MOBILE_BANKING, INTERNET_BANKING, BACKOFFICE]

### Channel-Specific Policy Rules
- **PIN Policy**: Only available for MOBILE_BANKING and INTERNET_BANKING
- **Password Policy**: Only available for INTERNET_BANKING and BACKOFFICE
- **MFA Policy**: Available for all channels
- **Security Question Policy**: Available for all channels

### Sub-Policy Creation
The specific policy entities (PinPolicyEntity, PasswordPolicyEntity, etc.) are created once per channel:
- Each sub-policy checks if it already exists for the specific PolicyEntity and Channel combination before creation
- This prevents duplicate sub-policies even when the seeder runs multiple times

