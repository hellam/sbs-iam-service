package ke.shiva.sbs_iam.config.seeder;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.OtpType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.TransactionMfaMode;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyScope;
import ke.shiva.sbs_iam.modules.iam.domain.enums.policy.PolicyType;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Database seeder to create initial policies in the system
 * Creates policies for PIN, Password, MFA, Security Questions for each channel
 * Also creates features and a global feature policy
 * <p>
 * To enable this seeder, add the following to your application.yaml:
 * seeder:
 * policy:
 * enabled: true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicySeeder implements CommandLineRunner {

    @Value("${seeder.policy.enabled:false}")
    private boolean seederEnabled;

    private final PolicyRepository policyRepository;
    private final PinPolicyRepository pinPolicyRepository;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final MfaPolicyRepository mfaPolicyRepository;
    private final SecurityQuestionPolicyRepository securityQuestionPolicyRepository;
    private final SessionPolicyRepository sessionPolicyRepository;
    private final FeatureRepository featureRepository;
    private final FeaturePolicyRepository featurePolicyRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seederEnabled) {
            log.debug("Policy seeder is disabled. Set 'seeder.policy.enabled=true' to enable it.");
            return;
        }

        log.info("Starting policy seeder...");

        try {
            // Create PIN policies - only for MOBILE_BANKING and INTERNET_BANKING
            for (Channel channel : new Channel[]{Channel.MOBILE_BANKING, Channel.INTERNET_BANKING}) {
                createPinPolicy(channel);
            }

            // Create PASSWORD policies - only for INTERNET_BANKING and BACKOFFICE
            for (Channel channel : new Channel[]{Channel.INTERNET_BANKING, Channel.BACKOFFICE}) {
                createPasswordPolicy(channel);
            }

            // Create MFA and Security Question policies for all channels
            for (Channel channel : new Channel[]{Channel.MOBILE_BANKING, Channel.INTERNET_BANKING, Channel.BACKOFFICE}) {
                createMfaPolicy(channel);
                createSecurityQuestionPolicy(channel);
                createSessionPolicy(channel);
            }

            // Create features and global feature policy
            createFeaturesAndGlobalPolicy();

            log.info("Policy seeder completed successfully!");

        } catch (Exception e) {
            log.error("Error running policy seeder", e);
            throw new RuntimeException("Failed to seed policy data", e);
        }
    }

    private void createPinPolicy(Channel channel) {
        log.info("Creating PIN policy for channel: {}", channel);

        PolicyEntity policy = upsertPolicy(PolicyType.PIN_POLICY, channel);

        Optional<PinPolicyEntity> existingPinPolicy =
                pinPolicyRepository.findByPolicyIdAndChannel(policy.getId(), channel);

        if (existingPinPolicy.isEmpty()) {
            log.info("Creating PinPolicyEntity for channel: {}", channel);
            PinPolicyEntity pinPolicy = new PinPolicyEntity();
            pinPolicy.setPolicy(policy);
            pinPolicy.setChannel(channel);
            setPinPolicyDefaults(pinPolicy);
            pinPolicyRepository.save(pinPolicy);
            return;
        }

        PinPolicyEntity pinPolicy = existingPinPolicy.get();
        if (setMissingPinPolicyDefaults(pinPolicy)) {
            log.info("Backfilling missing PIN policy defaults for channel: {}", channel);
            pinPolicyRepository.save(pinPolicy);
        } else {
            log.info("PinPolicyEntity for channel {} already exists", channel);
        }
    }

    private void createPasswordPolicy(Channel channel) {
        log.info("Creating Password policy for channel: {}", channel);

        PolicyEntity policy = upsertPolicy(PolicyType.PASSWORD_POLICY, channel);

        // Check if PasswordPolicyEntity already exists for this channel
        boolean passwordPolicyExists = passwordPolicyRepository.findAll().stream()
                .anyMatch(pp -> pp.getPolicy().getId().equals(policy.getId()) && pp.getChannel() == channel);

        if (!passwordPolicyExists) {
            log.info("Creating PasswordPolicyEntity for channel: {}", channel);
            PasswordPolicyEntity passwordPolicy = new PasswordPolicyEntity();
            passwordPolicy.setPolicy(policy);
            passwordPolicy.setChannel(channel);
            // Use default values
            passwordPolicy.setMinLength(Short.valueOf("8"));
            passwordPolicy.setMaxLength(Short.valueOf("32"));
            passwordPolicy.setRequireUppercase(true);
            passwordPolicy.setRequireLowercase(true);
            passwordPolicy.setRequireNumber(true);
            passwordPolicy.setRequireSymbol(false);
            passwordPolicy.setBlockCommonPasswords(true);
            passwordPolicy.setPasswordHistoryCount(Short.valueOf("5"));
            passwordPolicy.setExpirationEnabled(true);
            passwordPolicy.setExpirationDays(Short.valueOf("90"));
            passwordPolicy.setMaxFailedAttempts(Short.valueOf("5"));
            passwordPolicy.setLockoutMinutes(Short.valueOf("30"));
            passwordPolicy.setRequireFactoryReset(true);
            passwordPolicy.setHashAlgorithm("bcrypt");
            passwordPolicy.setHashCost(Short.valueOf("12"));
            passwordPolicyRepository.save(passwordPolicy);
        } else {
            log.info("PasswordPolicyEntity for channel {} already exists", channel);
        }
    }

    private void createMfaPolicy(Channel channel) {
        log.info("Creating MFA policy for channel: {}", channel);

        PolicyEntity policy = upsertPolicy(PolicyType.MFA_POLICY, channel);

        // Check if MfaPolicyEntity already exists for this channel
        boolean mfaPolicyExists = mfaPolicyRepository.findAll().stream()
                .anyMatch(mp -> mp.getPolicy().getId().equals(policy.getId()) && mp.getChannel() == channel);

        if (!mfaPolicyExists) {
            log.info("Creating MfaPolicyEntity for channel: {}", channel);
            MfaPolicyEntity mfaPolicy = new MfaPolicyEntity();
            mfaPolicy.setPolicy(policy);
            mfaPolicy.setChannel(channel);
            // Set specific MFA policy values
            mfaPolicy.setAllowedNotificationChannels(List.of("SMS"));
            mfaPolicy.setAllowTotp(false);
            mfaPolicy.setMaxVerifyAttempts((short) 3);
            mfaPolicy.setOtpType(OtpType.NUMERIC);
            mfaPolicy.setOtpLength((short) 6);
            mfaPolicy.setTransactionMfaMode(TransactionMfaMode.OTP);
            mfaPolicy.setEnforceOnTransactionInitiation(true);
            mfaPolicy.setEnforceOnTransactionApproval(true);
            mfaPolicy.setEnforceOnTransactionRejection(true);
            mfaPolicy.setOtpExpirySeconds(120);
            mfaPolicy.setOtpDailyLimit((short) 10);
            mfaPolicy.setEnforceOnNewDevice(true);
            mfaPolicy.setEnforceOnNewLocation(true);
            mfaPolicyRepository.save(mfaPolicy);
        } else {
            log.info("MfaPolicyEntity for channel {} already exists", channel);
        }
    }

    private void createSecurityQuestionPolicy(Channel channel) {
        log.info("Creating Security Question policy for channel: {}", channel);

        PolicyEntity policy = upsertPolicy(PolicyType.SEC_QN_POLICY, channel);

        Optional<SecurityQuestionPolicyEntity> existingSqPolicy =
                securityQuestionPolicyRepository.findByPolicyIdAndChannel(policy.getId(), channel);

        if (existingSqPolicy.isEmpty()) {
            log.info("Creating SecurityQuestionPolicyEntity for channel: {}", channel);
            SecurityQuestionPolicyEntity sqPolicy = new SecurityQuestionPolicyEntity();
            sqPolicy.setPolicy(policy);
            sqPolicy.setChannel(channel);
            setSecurityQuestionPolicyDefaults(sqPolicy);
            securityQuestionPolicyRepository.save(sqPolicy);
            return;
        }

        SecurityQuestionPolicyEntity sqPolicy = existingSqPolicy.get();
        if (setMissingSecurityQuestionPolicyDefaults(sqPolicy)) {
            log.info("Backfilling missing Security Question policy defaults for channel: {}", channel);
            securityQuestionPolicyRepository.save(sqPolicy);
        } else {
            log.info("SecurityQuestionPolicyEntity for channel {} already exists", channel);
        }
    }

    private void createSessionPolicy(Channel channel) {
        log.info("Creating Session policy for channel: {}", channel);

        PolicyEntity policy = upsertPolicy(PolicyType.SESSION_POLICY, channel);

        Optional<SessionPolicyEntity> existingSessionPolicy =
                sessionPolicyRepository.findByPolicyIdAndChannel(policy.getId(), channel);

        if (existingSessionPolicy.isEmpty()) {
            log.info("Creating SessionPolicyEntity for channel: {}", channel);
            SessionPolicyEntity sessionPolicy = new SessionPolicyEntity();
            sessionPolicy.setPolicy(policy);
            sessionPolicy.setChannel(channel);
            setSessionPolicyDefaults(sessionPolicy);
            sessionPolicyRepository.save(sessionPolicy);
            return;
        }

        SessionPolicyEntity sessionPolicy = existingSessionPolicy.get();
        if (setMissingSessionPolicyDefaults(sessionPolicy)) {
            log.info("Backfilling missing Session policy defaults for channel: {}", channel);
            sessionPolicyRepository.save(sessionPolicy);
        } else {
            log.info("SessionPolicyEntity for channel {} already exists", channel);
        }
    }

    private void setSessionPolicyDefaults(SessionPolicyEntity sessionPolicy) {
        sessionPolicy.setInactivityTimeoutSeconds(180);
        sessionPolicy.setWarningCountdownSeconds(60);
    }

    private boolean setMissingSessionPolicyDefaults(SessionPolicyEntity sessionPolicy) {
        boolean updated = false;

        if (sessionPolicy.getInactivityTimeoutSeconds() == null) {
            sessionPolicy.setInactivityTimeoutSeconds(180);
            updated = true;
        }
        if (sessionPolicy.getWarningCountdownSeconds() == null) {
            sessionPolicy.setWarningCountdownSeconds(60);
            updated = true;
        }

        return updated;
    }

    private void setPinPolicyDefaults(PinPolicyEntity pinPolicy) {
        pinPolicy.setMinLength((short) 4);
        pinPolicy.setMaxLength((short) 6);
        pinPolicy.setPinHistoryCount((short) 5);
        pinPolicy.setBlockSequential(true);
        pinPolicy.setBlockRepeating(true);
        pinPolicy.setMaxFailedAttempts((short) 5);
        pinPolicy.setLockoutMinutes((short) 30);
        pinPolicy.setHashAlgorithm("bcrypt");
        pinPolicy.setHashCost((short) 10);
    }

    private boolean setMissingPinPolicyDefaults(PinPolicyEntity pinPolicy) {
        boolean updated = false;

        if (pinPolicy.getMinLength() == null) {
            pinPolicy.setMinLength((short) 4);
            updated = true;
        }
        if (pinPolicy.getMaxLength() == null) {
            pinPolicy.setMaxLength((short) 6);
            updated = true;
        }
        if (pinPolicy.getPinHistoryCount() == null) {
            pinPolicy.setPinHistoryCount((short) 5);
            updated = true;
        }
        if (pinPolicy.getBlockSequential() == null) {
            pinPolicy.setBlockSequential(true);
            updated = true;
        }
        if (pinPolicy.getBlockRepeating() == null) {
            pinPolicy.setBlockRepeating(true);
            updated = true;
        }
        if (pinPolicy.getMaxFailedAttempts() == null) {
            pinPolicy.setMaxFailedAttempts((short) 5);
            updated = true;
        }
        if (pinPolicy.getLockoutMinutes() == null) {
            pinPolicy.setLockoutMinutes((short) 30);
            updated = true;
        }
        if (pinPolicy.getHashAlgorithm() == null || pinPolicy.getHashAlgorithm().isBlank()) {
            pinPolicy.setHashAlgorithm("bcrypt");
            updated = true;
        }
        if (pinPolicy.getHashCost() == null) {
            pinPolicy.setHashCost((short) 10);
            updated = true;
        }

        return updated;
    }

    private void setSecurityQuestionPolicyDefaults(SecurityQuestionPolicyEntity sqPolicy) {
        sqPolicy.setEnabled(false);
        sqPolicy.setMinQuestions((short) 0);
        sqPolicy.setMaxQuestions((short) 0);
        sqPolicy.setMandatory(false);
        sqPolicy.setAskOnForgotPassword(false);
        sqPolicy.setAskOnSensitiveAction(false);
        sqPolicy.setIsActive(true);
        sqPolicy.setMaxVerifyAttempts((short) 3);
    }

    private boolean setMissingSecurityQuestionPolicyDefaults(SecurityQuestionPolicyEntity sqPolicy) {
        boolean updated = false;

        if (sqPolicy.getEnabled() == null) {
            sqPolicy.setEnabled(false);
            updated = true;
        }
        if (sqPolicy.getMinQuestions() == null) {
            sqPolicy.setMinQuestions((short) 0);
            updated = true;
        }
        if (sqPolicy.getMaxQuestions() == null) {
            sqPolicy.setMaxQuestions((short) 0);
            updated = true;
        }
        if (sqPolicy.getMandatory() == null) {
            sqPolicy.setMandatory(false);
            updated = true;
        }
        if (sqPolicy.getAskOnForgotPassword() == null) {
            sqPolicy.setAskOnForgotPassword(false);
            updated = true;
        }
        if (sqPolicy.getAskOnSensitiveAction() == null) {
            sqPolicy.setAskOnSensitiveAction(false);
            updated = true;
        }
        if (sqPolicy.getIsActive() == null) {
            sqPolicy.setIsActive(true);
            updated = true;
        }
        if (sqPolicy.getMaxVerifyAttempts() == null) {
            sqPolicy.setMaxVerifyAttempts((short) 3);
            updated = true;
        }

        return updated;
    }

    private PolicyEntity upsertPolicy(PolicyType policyType, Channel channel) {
        // Reuse policy if and only if we already have a record for this exact channel
        Optional<PolicyEntity> existingPolicy = policyRepository
                .findByPolicyTypeAndChannelsContaining(policyType, channel.name())
                .stream()
                .findFirst();

        if (existingPolicy.isPresent()) {
            log.info("{} policy already contains channel {}. Skipping...", policyType, channel);
            return existingPolicy.get();
        }

        log.info("Creating new {} policy for channel: {}", policyType, channel);
        PolicyEntity newPolicy = new PolicyEntity();
        newPolicy.setPublicId(UUID.randomUUID());
        newPolicy.setPolicyType(policyType);
        newPolicy.setChannels(new Channel[]{channel});
        newPolicy.setName(policyType.getValue());
        newPolicy.setDescription(policyType.getValue() + " configuration");
        return policyRepository.save(newPolicy);
    }

    private void createFeaturesAndGlobalPolicy() {
        log.info("Creating features and global feature policy");

        // Create features
        FeatureEntity rtgsFeature = featureRepository.findByCode("RTGS")
                .orElseGet(() -> {
                    FeatureEntity f = new FeatureEntity();
                    f.setCode("RTGS");
                    f.setChannel(Channel.INTERNET_BANKING);
                    f.setName("RTGS Transfer");
                    f.setDescription("Allows users to transfer funds via RTGS");
                    f.setCategory("Transfers");
                    f.setIsTransaction(true);
                    return featureRepository.save(f);
                });

        FeatureEntity swiftFeature = featureRepository.findByCode("SWIFT")
                .orElseGet(() -> {
                    FeatureEntity f = new FeatureEntity();
                    f.setCode("SWIFT");
                    f.setChannel(Channel.INTERNET_BANKING);
                    f.setName("SWIFT Transfer");
                    f.setDescription("Allows users to transfer funds via SWIFT");
                    f.setCategory("Transfers");
                    f.setIsTransaction(true);
                    return featureRepository.save(f);
                });

        // Create global feature policy
        if (featurePolicyRepository.findByNameAndChannelAndPolicyScope("Global Feature Policy", Channel.BACKOFFICE, PolicyScope.GLOBAL).isEmpty()) {
            FeaturePolicyEntity globalPolicy = new FeaturePolicyEntity();
            globalPolicy.setName("Global Feature Policy");
            globalPolicy.setDescription("Global feature policy with basic features");

            Set<Long> features = new HashSet<>();
            features.add(rtgsFeature.getId());
            features.add(swiftFeature.getId());

            globalPolicy.setFeatures(features);
            globalPolicy.setChannel(Channel.BACKOFFICE); // Channel can be arbitrary for global policy
            globalPolicy.setPolicyScope(PolicyScope.GLOBAL);
            globalPolicy.setIsActive(true);
            featurePolicyRepository.save(globalPolicy);
            log.info("Created global feature policy with {} features", features.size());
        } else {
            log.info("Global feature policy already exists");
        }
    }
}
