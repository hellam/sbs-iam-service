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
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
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
    private final PolicyService policyService;
    private final CustomerAuthRepository customerAuthRepository;
    private final EmployeeAuthRepository employeeAuthRepository;

    // Optional: if using OTP, trigger it here
    public String initiate(MfaInitRequest req, UUID flowId)  {

        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.PASSWORD_OK);

        LoginRequirements reqs = loginFlowService.getRequirements(session);

        if (reqs.isTotpRequired()) {
            return "Please provide TOTP code from your authenticator app.";
        }

        // If OTP is required, send it
        if (reqs.isOtpRequired()) {
            if (!policyService.isAllowedNotificationChannel(session.getChannel(), req.getChannel())) {
                throw BaseException.badRequest("Selected OTP channel is not allowed for this login.");
            }
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
        MfaVerifyResponse resp = new MfaVerifyResponse();

        // If channel policy allows TOTP and the user is not enrolled yet, redirect to enrollment before profile/token stages.
        if (shouldRequireTotpEnrollment(session, reqs)) {
            loginFlowService.updateStage(session, LoginStage.TOTP_ENROLL_REQUIRED);
            loginFlowService.extend(session);
            resp.setTotpEnrollmentRequired(true);
            resp.setNextIsProfileSelection(false);
            resp.setReloginRequired(false);
            return resp;
        }

        loginFlowService.updateStage(session, LoginStage.MFA_OK);
        loginFlowService.extend(session);
        resp.setTotpEnrollmentRequired(false);
        resp.setReloginRequired(false);
        resp.setNextIsProfileSelection(loginFlowService.getRequirements(session).nextIsProfileSelection());

        return resp;
    }

    private boolean shouldRequireTotpEnrollment(SessionEntity session, LoginRequirements reqs) {
        if (!reqs.isOtpRequired() || reqs.isTotpRequired()) {
            return false;
        }
        MfaPolicyEntity policy = policyService.getMfaPolicy(session.getChannel());
        if (policy == null || !Boolean.TRUE.equals(policy.getAllowTotp())) {
            return false;
        }

        return switch (session.getChannel()) {
            case INTERNET_BANKING, MOBILE_BANKING -> {
                var auth = customerAuthRepository.findByIamUser(session.getIamUser());
                yield auth == null
                        || !Boolean.TRUE.equals(auth.getMfaEnabled())
                        || auth.getMfaSecret() == null
                        || auth.getMfaSecret().isBlank();
            }
            case BACKOFFICE -> {
                var auth = employeeAuthRepository.findByIamUser(session.getIamUser());
                yield auth == null
                        || !Boolean.TRUE.equals(auth.getMfaEnabled())
                        || auth.getMfaSecret() == null
                        || auth.getMfaSecret().isBlank();
            }
            default -> false;
        };
    }

    public MfaPolicyResponse getMfaPolicy(Channel channel) {
        MfaPolicyEntity policy = policyService.getMfaPolicy(channel);
        if (policy == null) {
            throw BaseException.notFound("MFA policy not found for channel: " + channel);
        }

        return MfaPolicyResponse.builder()
                .channel(policy.getChannel())
                .allowedNotificationChannels(policyService.getAllowedNotificationChannels(channel))
                .allowTotp(policy.getAllowTotp())
                .maxVerifyAttempts(policy.getMaxVerifyAttempts())
                .otpType(policy.getOtpType())
                .otpLength(policy.getOtpLength())
                .transactionMfaMode(policy.getTransactionMfaMode())
                .enforceOnTransactionInitiation(policy.getEnforceOnTransactionInitiation())
                .enforceOnTransactionApproval(policy.getEnforceOnTransactionApproval())
                .enforceOnTransactionRejection(policy.getEnforceOnTransactionRejection())
                .otpExpirySeconds(policy.getOtpExpirySeconds())
                .enforceOnNewDevice(policy.getEnforceOnNewDevice())
                .enforceOnNewLocation(policy.getEnforceOnNewLocation())
                .build();
    }
}
