package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaInitResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final LoginFlowService loginFlowService;
//    private final TotpVerifier totpVerifier;
//    private final OtpService otpService;
//    private final SecurityEventService securityEventService;

    // Optional: if using OTP, trigger it here
    public MfaInitResponse initiate(MfaInitRequest req)  {

        SessionEntity session = loginFlowService.requireStage(req.getFlowId(), LoginStage.PASSWORD_OK);

        LoginRequirements reqs = loginFlowService.getRequirements(session);

        if (!reqs.isMfaRequired()) {
            new MfaInitResponse(req.getFlowId());
        }

        // If TOTP is enabled, no need to send OTP
//        if (!session.getIamUser().hasTotpSecret()) {
//            otpService.sendOtp(session);
//        }

        return new MfaInitResponse(req.getFlowId());
    }

    public MfaVerifyResponse verify(MfaVerifyRequest req) {

        SessionEntity session = loginFlowService.requireStage(req.getFlowId(), LoginStage.PASSWORD_OK);
        IamUserEntity user = session.getIamUser();

        boolean ok;

//        if (user.hasTotpSecret()) {
//            ok = totpVerifier.verify(user, req.getCode());
//        } else {
//            ok = otpService.verify(req.getFlowId(), req.getCode());
//        }
//
//        if (!ok) {
//            securityEventService.onLoginFailure(user, "MFA_INVALID", session);
//            throw new AuthException("Invalid MFA code");
//        }
//
//        securityEventService.onLoginSuccess(user, "MFA_SUCCESS", session);

        loginFlowService.updateStage(session, LoginStage.MFA_OK);

        MfaVerifyResponse resp = new MfaVerifyResponse();
        resp.setFlowId(UUID.fromString(session.getSessionId()));
        resp.setNextIsProfileSelection(loginFlowService
                .getRequirements(session)
                .isProfileSelectionRequired());

        return resp;
    }
}
