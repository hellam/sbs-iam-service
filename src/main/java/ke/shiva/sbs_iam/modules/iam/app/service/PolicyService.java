package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SecurityQuestionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.NotificationChannel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.MfaPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final MfaPolicyRepository mfaPolicyRepository;
    private final SecurityQuestionPolicyRepository securityQuestionPolicyRepository;

    public MfaPolicyEntity getMfaPolicy(Channel channel) {

        MfaPolicyEntity mfaPolicy = mfaPolicyRepository.findByChannel(channel);
        if (mfaPolicy == null) {
            log.warn("No MFA policy found for channel: {}", channel);
            return null;
        }

        return mfaPolicy;
    }

    public List<String> getAllowedNotificationChannels(Channel channel) {
        MfaPolicyEntity mfaPolicy = getMfaPolicy(channel);
        if (mfaPolicy == null || mfaPolicy.getAllowedNotificationChannels() == null) {
            return List.of();
        }

        return mfaPolicy.getAllowedNotificationChannels().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(this::isKnownNotificationChannel)
                .distinct()
                .toList();
    }

    public boolean isAllowedNotificationChannel(Channel channel, NotificationChannel notificationChannel) {
        return getAllowedNotificationChannels(channel).contains(notificationChannel.name());
    }

    public SecurityQuestionPolicyEntity getSecurityQuestionPolicy(Channel channel) {

        // Assuming you have a SecurityQuestionPolicyRepository similar to MfaPolicyRepository
        SecurityQuestionPolicyEntity questionPolicy = securityQuestionPolicyRepository.findByChannel(channel);
        if (questionPolicy == null) {
            log.warn("No Security Question policy found for channel: {}", channel);
            return null;
        }

        return questionPolicy;
    }

    private boolean isKnownNotificationChannel(String value) {
        return Arrays.stream(NotificationChannel.values())
                .anyMatch(channel -> channel.name().equals(value));
    }
}
