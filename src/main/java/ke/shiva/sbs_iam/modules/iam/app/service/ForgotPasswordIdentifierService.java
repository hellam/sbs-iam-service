package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.ForgotPasswordIdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.ForgotPasswordIdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.domain.model.ForgotPasswordRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordIdentifierService {

    private final LoginIdentifierRepository identifierRepo;
    private final PolicyEvaluationService policyEvaluationService;
    private final ForgotPasswordFlowService flowService;

    @Transactional
    public ForgotPasswordIdentifierResponse handle(ForgotPasswordIdentifierRequest request, String deviceId) {
        // Lookup user by identifier
        LoginIdentifierEntity identifier = identifierRepo
                .findByIdentifierAndChannelAndStatus(request.getIdentifier(), request.getChannel(), IamStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("Forgot password attempt for non-existent identifier: {}", request.getIdentifier());
                    // Don't reveal if user exists or not for security reasons
                    return BaseException.badRequest();
                });

        IamUserEntity user = identifier.getIamUser();

        // Check if account is active
        if (user.getStatus() != IamStatus.ACTIVE) {
            log.warn("Forgot password attempt for inactive account: {}", request.getIdentifier());
            throw BaseException.unauthorized("Account is inactive. Please contact support.");
        }

        // Evaluate requirements
        ForgotPasswordRequirements requirements = policyEvaluationService
                .evaluateForgotPasswordRequirements(user, request.getChannel());

        // Start forgot password flow
        SessionEntity session = flowService.start(
                user,
                request.getChannel(),
                requirements,
                request.getIdentifier(),
                deviceId
        );

        // Determine next step
        String nextStep = determineNextStep(requirements);

        return ForgotPasswordIdentifierResponse.builder()
                .flowId(UUID.fromString(session.getSessionId()))
                .securityQuestionsRequired(requirements.isSecurityQuestionsRequired())
                .securityQuestionsCount(requirements.getSecurityQuestionsCount())
                .mfaRequired(requirements.isMfaRequired())
                .nextStep(nextStep)
                .build();
    }

    private String determineNextStep(ForgotPasswordRequirements requirements) {
        if (requirements.isSecurityQuestionsRequired()) {
            return "SECURITY_QUESTIONS";
        } else if (requirements.isMfaRequired()) {
            return "MFA";
        } else {
            return "RESET_PASSWORD";
        }
    }
}
