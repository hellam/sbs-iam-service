package ke.shiva.sbs_iam.modules.iam.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.ForgotPasswordRequirements;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordMfaService {

    private final ForgotPasswordFlowService flowService;
    private final CommonMfaService commonMfaService;
    private final ObjectMapper objectMapper;

    @Transactional
    public String initiate(MfaInitRequest request, UUID flowId) {
        // Get and validate session - must be at least at FP_IDENTIFIER_OK
        // Could be FP_IDENTIFIER_OK (no security questions) or FP_SECURITY_QUESTIONS_OK
        SessionEntity session = flowService.requireAtLeast(flowId, LoginStage.FP_SEC_QNS_OK);

        // Validate current stage
        LoginStage currentStage = session.getStatus();
        if (currentStage != LoginStage.FP_IDENTIFIER_OK && currentStage != LoginStage.FP_SEC_QNS_OK) {
            throw BaseException.invalidStage();
        }

        // Get requirements from metadata
        ForgotPasswordRequirements requirements = objectMapper.convertValue(
                session.getMetadata().get("requirements"),
                ForgotPasswordRequirements.class
        );

        if (!requirements.isMfaRequired()) {
            log.warn("MFA not required but attempted for session: {}", session.getSessionId());
            throw BaseException.badRequest("MFA is not required for this flow");
        }

        // Send OTP using common MFA service
        // Note: TOTP is typically not supported for forgot password (user might not have access to their device)
        return commonMfaService.sendOtp(session, request.getChannel());
    }

    @Transactional
    public MfaVerifyResponse verify(MfaVerifyRequest request, UUID flowId) {
        // Get and validate session
        SessionEntity session = flowService.requireAtLeast(flowId, LoginStage.FP_SEC_QNS_OK);

        // Validate current stage
        LoginStage currentStage = session.getStatus();
        if (currentStage != LoginStage.FP_IDENTIFIER_OK && currentStage != LoginStage.FP_SEC_QNS_OK) {
            throw BaseException.invalidStage();
        }

        // Get requirements from metadata
        ForgotPasswordRequirements requirements = objectMapper.convertValue(
                session.getMetadata().get("requirements"),
                ForgotPasswordRequirements.class
        );

        if (!requirements.isMfaRequired()) {
            throw BaseException.badRequest("MFA is not required for this flow");
        }

        // Verify OTP using common MFA service
        // For forgot password, we only support OTP (not TOTP) for security reasons
        boolean valid = commonMfaService.verifyOtp(session.getSessionId(), request.getCode());

        if (!valid) {
            log.warn("Invalid MFA code for forgot password session: {}", session.getSessionId());
            throw BaseException.badRequest("Invalid code");
        }

        // Update stage to FP_MFA_OK
        flowService.updateStage(session, LoginStage.FP_MFA_OK);

        MfaVerifyResponse response = new MfaVerifyResponse();
        response.setNextIsProfileSelection(false); // Forgot password doesn't require profile selection

        return response;
    }
}
