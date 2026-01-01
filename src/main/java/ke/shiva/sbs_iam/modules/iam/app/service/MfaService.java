package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaInitResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final LoginFlowService loginFlowService;
    private final OtpService otpService;
    private final TotpVerifier totpVerifier;
    private final SecurityEventService securityEventService;
    private final LoginHistoryService loginHistoryService;

    // Optional: if using OTP, trigger it here
    public MfaInitResponse initiate(MfaInitRequest req, UUID flowId)  {

        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.PASSWORD_OK);

        LoginRequirements reqs = loginFlowService.getRequirements(session);

        if (reqs.isTotpRequired()) {
            return new MfaInitResponse(flowId);
        }


        // If OTP is required, send it
        if (reqs.isOtpRequired()) {
            otpService.sendOtp(session, req.getChannel());
        }

        return new MfaInitResponse(flowId);
    }

    public MfaVerifyResponse verify(MfaVerifyRequest req, UUID flowId) {

        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.PASSWORD_OK);
        IamUserEntity user = session.getIamUser();
        LoginRequirements reqs = loginFlowService.getRequirements(session);

        // Extract identifier from session metadata
        String identifier = loginFlowService.extractIdentifier(session);

        boolean ok;

        if (reqs.isTotpRequired()) {
            ok = totpVerifier.verify(user, req.getCode());
        } else {
            ok = otpService.verify(flowId.toString(), req.getCode());
        }

        if (!ok) {
            securityEventService.onLoginFailure(user, "MFA_INVALID", session);
            loginHistoryService.logMfaFailure(user, identifier, session, "MFA_INVALID");
            throw BaseException.badRequest("Invalid code");
        }

        securityEventService.onLoginSuccess(user, "MFA_SUCCESS", session);
        loginHistoryService.logMfaSuccess(user, identifier, session);

        loginFlowService.updateStage(session, LoginStage.MFA_OK);
        loginFlowService.extend(session);

        MfaVerifyResponse resp = new MfaVerifyResponse();
        resp.setFlowId(UUID.fromString(session.getSessionId()));
        resp.setNextIsProfileSelection(loginFlowService
                .getRequirements(session)
                .isProfileSelectionRequired());

        return resp;
    }
}
