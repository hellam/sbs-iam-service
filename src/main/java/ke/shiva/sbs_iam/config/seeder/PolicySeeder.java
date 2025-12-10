package ke.shiva.sbs_iam.config.seeder;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.*;
import ke.shiva.sbs_iam.modules.iam.domain.entity.system.FeatureEntity;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Database seeder to create initial policies in the system
 * Creates policies for PIN, Password, MFA, Security Questions for each channel
 * Also creates features and a global feature policy
 *
 * To enable this seeder, add the following to your application.yaml:
 * seeder:
 *   policy:
 *     enabled: true
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
            // Create policies for each channel
            for (Channel channel : new Channel[]{Channel.MOBILE_BANKING, Channel.INTERNET_BANKING, Channel.BACKOFFICE}) {
                createPinPolicy(channel);
                createPasswordPolicy(channel);
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

        PolicyEntity policy = new PolicyEntity();
        policy.setPublicId(UUID.randomUUID());
        policy.setPolicyType(PolicyType.PIN_POLICY);
        policy.setChannels(new Channel[]{channel});
        policy.setName("PIN Policy for " + channel.getDescription());
        policy.setDescription("PIN policy configuration for " + channel.getDescription());
        policy = policyRepository.save(policy);

        PinPolicyEntity pinPolicy = new PinPolicyEntity();
        pinPolicy.setPolicy(policy);
        pinPolicy.setChannel(channel);
        // Use default values from entity
        pinPolicyRepository.save(pinPolicy);
    }

    private void createPasswordPolicy(Channel channel) {
        log.info("Creating Password policy for channel: {}", channel);

        PolicyEntity policy = new PolicyEntity();
        policy.setPublicId(UUID.randomUUID());
        policy.setPolicyType(PolicyType.PASSWORD_POLICY);
        policy.setChannels(new Channel[]{channel});
        policy.setName("Password Policy for " + channel.getDescription());
        policy.setDescription("Password policy configuration for " + channel.getDescription());
        policy = policyRepository.save(policy);

        PasswordPolicyEntity passwordPolicy = new PasswordPolicyEntity();
        passwordPolicy.setPolicy(policy);
        passwordPolicy.setChannel(channel);
        // Use default values
        passwordPolicyRepository.save(passwordPolicy);
    }

    private void createMfaPolicy(Channel channel) {
        log.info("Creating MFA policy for channel: {}", channel);

        PolicyEntity policy = new PolicyEntity();
        policy.setPublicId(UUID.randomUUID());
        policy.setPolicyType(PolicyType.MFA_POLICY);
        policy.setChannels(new Channel[]{channel});
        policy.setName("MFA Policy for " + channel.getDescription());
        policy.setDescription("MFA policy configuration for " + channel.getDescription());
        policy = policyRepository.save(policy);

        MfaPolicyEntity mfaPolicy = new MfaPolicyEntity();
        mfaPolicy.setPolicy(policy);
        mfaPolicy.setChannel(channel);
        // Use default values
        mfaPolicyRepository.save(mfaPolicy);
    }

    private void createSecurityQuestionPolicy(Channel channel) {
        log.info("Creating Security Question policy for channel: {}", channel);

        PolicyEntity policy = new PolicyEntity();
        policy.setPublicId(UUID.randomUUID());
        policy.setPolicyType(PolicyType.SEC_QN_POLICY);
        policy.setChannels(new Channel[]{channel});
        policy.setName("Security Question Policy for " + channel.getDescription());
        policy.setDescription("Security question policy configuration for " + channel.getDescription());
        policy = policyRepository.save(policy);

        SecurityQuestionPolicyEntity sqPolicy = new SecurityQuestionPolicyEntity();
        sqPolicy.setPolicy(policy);
        sqPolicy.setChannel(channel);
        // Use default values
        securityQuestionPolicyRepository.save(sqPolicy);
    }

    private void createFeaturesAndGlobalPolicy() {
        log.info("Creating features and global feature policy");

        // Create features
        FeatureEntity loginFeature = featureRepository.findByCode("LOGIN")
                .orElseGet(() -> {
                    FeatureEntity f = new FeatureEntity();
                    f.setCode("LOGIN");
                    f.setName("User Login");
                    f.setDescription("Allows users to log in to the system");
                    f.setCategory("Authentication");
                    return featureRepository.save(f);
                });

        FeatureEntity transferFeature = featureRepository.findByCode("TRANSFER")
                .orElseGet(() -> {
                    FeatureEntity f = new FeatureEntity();
                    f.setCode("TRANSFER");
                    f.setName("Fund Transfer");
                    f.setDescription("Allows users to transfer funds");
                    f.setCategory("Transactions");
                    return featureRepository.save(f);
                });

        // Create global feature policy
        if (featurePolicyRepository.findByNameAndChannelAndPolicyScope("Global Feature Policy", Channel.BACKOFFICE, PolicyScope.GLOBAL).isEmpty()) {
            FeaturePolicyEntity globalPolicy = new FeaturePolicyEntity();
            globalPolicy.setName("Global Feature Policy");
            globalPolicy.setDescription("Global feature policy with basic features");
            Map<Long, FeatureEntity> features = new HashMap<>();
            features.put(loginFeature.getId(), loginFeature);
            features.put(transferFeature.getId(), transferFeature);
            globalPolicy.setFeatures(features);
            globalPolicy.setChannel(Channel.BACKOFFICE); // For global, using BACKOFFICE as default
            globalPolicy.setPolicyScope(PolicyScope.GLOBAL);
            globalPolicy.setIsActive(true);
            featurePolicyRepository.save(globalPolicy);
            log.info("Created global feature policy with {} features", features.size());
        } else {
            log.info("Global feature policy already exists");
        }
    }
}
