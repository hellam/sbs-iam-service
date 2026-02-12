package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaPolicyResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.MfaPolicyRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final LoginFlowService loginFlowService;
    private final CommonMfaService commonMfaService;
    private final SecurityEventService securityEventService;
    private final LoginHistoryService loginHistoryService;
    private final MfaPolicyRepository mfaPolicyRepository;

    // Optional: if using OTP, trigger it here
    public String initiate(MfaInitRequest req, UUID flowId)  {

        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.PASSWORD_OK);

        LoginRequirements reqs = loginFlowService.getRequirements(session);

        if (reqs.isTotpRequired()) {
            return "Please provide TOTP code from your authenticator app.";
        }

        // If OTP is required, send it
        if (reqs.isOtpRequired()) {
            return commonMfaService.sendOtp(session, req.getChannel());
        }

        return "";
    }

    public MfaVerifyResponse verify(MfaVerifyRequest req, UUID flowId) {

        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.PASSWORD_OK);
        IamUserEntity user = session.getIamUser();
        LoginRequirements reqs = loginFlowService.getRequirements(session);

        // Extract identifier from session metadata
        String identifier = loginFlowService.extractIdentifier(session);

        // Verify MFA code using common service
        boolean ok = commonMfaService.verifyMfaCode(session, req.getCode(), reqs.isTotpRequired());

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
        resp.setNextIsProfileSelection(loginFlowService.getRequirements(session).nextIsProfileSelection());

        return resp;
    }

    public MfaPolicyResponse getMfaPolicy(Channel channel) {
        MfaPolicyEntity policy = mfaPolicyRepository.findByChannel(channel);
        if (policy == null) {
            throw BaseException.notFound("MFA policy not found for channel: " + channel);
        }

        return MfaPolicyResponse.builder()
                .channel(policy.getChannel())
                .allowedNotificationChannels(policy.getAllowedNotificationChannels())
                .allowTotp(policy.getAllowTotp())
                .maxVerifyAttempts(policy.getMaxVerifyAttempts())
                .otpType(policy.getOtpType())
                .otpLength(policy.getOtpLength())
                .otpExpirySeconds(policy.getOtpExpirySeconds())
                .enforceOnNewDevice(policy.getEnforceOnNewDevice())
                .enforceOnNewLocation(policy.getEnforceOnNewLocation())
                .build();
    }
}
