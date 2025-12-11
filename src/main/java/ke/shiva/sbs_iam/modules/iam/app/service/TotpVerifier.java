package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import org.springframework.stereotype.Service;

@Service
public class TotpVerifier {
    public boolean verify(IamUserEntity user, String code) {
        // TODO: Implement TOTP verification
        return false;
    }
}

