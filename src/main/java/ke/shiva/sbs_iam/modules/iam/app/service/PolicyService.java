package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SecurityQuestionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.MfaPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public SecurityQuestionPolicyEntity getSecurityQuestionPolicy(Channel channel) {

        // Assuming you have a SecurityQuestionPolicyRepository similar to MfaPolicyRepository
        SecurityQuestionPolicyEntity questionPolicy = securityQuestionPolicyRepository.findByChannel(channel);
        if (questionPolicy == null) {
            log.warn("No Security Question policy found for channel: {}", channel);
            return null;
        }

        return questionPolicy;
    }
}
