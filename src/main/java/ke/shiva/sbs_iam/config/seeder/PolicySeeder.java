package ke.shiva.sbs_iam.config.seeder;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.OtpType;
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

        // Check if PinPolicyEntity already exists for this channel
        boolean pinPolicyExists = pinPolicyRepository.findAll().stream()
                .anyMatch(pp -> pp.getPolicy().getId().equals(policy.getId()) && pp.getChannel() == channel);

        if (!pinPolicyExists) {
            log.info("Creating PinPolicyEntity for channel: {}", channel);
            PinPolicyEntity pinPolicy = new PinPolicyEntity();
            pinPolicy.setPolicy(policy);
            pinPolicy.setChannel(channel);
            // Use default values from entity
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

        // Check if SecurityQuestionPolicyEntity already exists for this channel
        boolean sqPolicyExists = securityQuestionPolicyRepository.findAll().stream()
                .anyMatch(sqp -> sqp.getPolicy().getId().equals(policy.getId()) && sqp.getChannel() == channel);

        if (!sqPolicyExists) {
            log.info("Creating SecurityQuestionPolicyEntity for channel: {}", channel);
            SecurityQuestionPolicyEntity sqPolicy = new SecurityQuestionPolicyEntity();
            sqPolicy.setPolicy(policy);
            sqPolicy.setChannel(channel);
            // Use default values
            securityQuestionPolicyRepository.save(sqPolicy);
        } else {
            log.info("SecurityQuestionPolicyEntity for channel {} already exists", channel);
        }
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
