package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    public MfaPolicyEntity getMfaPolicy(Channel channel) {
        // TODO: Implement logic to retrieve MFA policy from the database
        return new MfaPolicyEntity();
    }
}
