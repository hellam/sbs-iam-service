package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaVerifyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final LoginFlowService flowService;
    private final TotpVerifier totpVerifier;
    private final OtpService otpService;
    private final SecurityEventService eventService;

    public MfaInitResponse initiate(MfaInitRequest req) {
        Session s = flowService.requireStage(req.getFlowId(), LoginStage.PASSWORD_OK);
        if (!s.getRequirements().isMfaRequired()) {
            throw new AuthException("MFA not required");
        }
        otpService.sendOtpIfNeeded(s.getUser());
        return new MfaInitResponse(req.getFlowId());
    }

    public MfaVerifyResponse verify(MfaVerifyRequest req) {
        Session s = flowService.requireStage(req.getFlowId(), LoginStage.PASSWORD_OK);

        boolean ok;
        if (s.getUser().hasTotpSetup())
            ok = totpVerifier.verify(s.getUser(), req.getCode());
        else
            ok = otpService.verify(req.getFlowId(), req.getCode());

        if (!ok)
            throw new AuthException("Invalid MFA code");

        flowService.updateStage(s, LoginStage.MFA_OK);
        eventService.success(s.getUser(), "MFA_SUCCESS");

        return new MfaVerifyResponse(s.getId(), s.getRequirements().isProfileSelectionRequired());
    }
}

