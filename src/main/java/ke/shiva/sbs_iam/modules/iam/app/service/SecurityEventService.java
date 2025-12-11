package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import org.springframework.stereotype.Service;

@Service
public class SecurityEventService {

    public void onLoginFailure(IamUserEntity user, String reason, SessionEntity session) {
        // TODO: Implement login failure event logging
    }

    public void onLoginSuccess(IamUserEntity user, String reason, SessionEntity session) {
        // TODO: Implement login success event logging
    }
}

