package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.ForgotPasswordIdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.ForgotPasswordIdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.SecurityQuestionsResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.IamUserSecurityQuestionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityQuestionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.domain.model.ForgotPasswordRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserSecurityQuestionRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import ke.shiva.shivacorestarter.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordIdentifierService {

    private final LoginIdentifierRepository identifierRepo;
    private final PolicyEvaluationService policyEvaluationService;
    private final ForgotPasswordFlowService flowService;
    private final IamUserSecurityQuestionRepository iamUserSecurityQuestionRepository;
    private final EncryptionUtil encryptionUtil;
    private final ChannelIdentifierNormalizer identifierNormalizer;

    @Value("${shiva.security.spa.public-key}")
    private String spaPublicKey;

    @Transactional
    public ForgotPasswordIdentifierResponse handle(ForgotPasswordIdentifierRequest request, String deviceId) {
        String lookupIdentifier = identifierNormalizer.normalize(request.getIdentifier(), request.getChannel());

        // Lookup user by identifier
        LoginIdentifierEntity identifier = identifierRepo
                .findByIdentifierAndChannelAndStatus(lookupIdentifier, request.getChannel(), IamStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("Forgot password attempt for non-existent identifier: {}", lookupIdentifier);
                    // Don't reveal if user exists or not for security reasons
                    return BaseException.badRequest();
                });

        IamUserEntity user = identifier.getIamUser();

        // Check if account is active
        if (user.getStatus() != IamStatus.ACTIVE) {
            log.warn("Forgot password attempt for inactive account: {}", lookupIdentifier);
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
                lookupIdentifier,
                deviceId
        );

        // Determine next step
        String nextStep = determineNextStep(requirements);

        //IAM User security Questions
        List<IamUserSecurityQuestionEntity> userSecurityQuestion =
                iamUserSecurityQuestionRepository.findAllByIamUserId(user.getId());
        if (userSecurityQuestion.isEmpty()) {
            log.warn("Forgot password attempt but no security questions set for user: {}", lookupIdentifier);
            throw BaseException.badRequest("Security questions are required but not set for this user.");
        }

        List<SecurityQuestionEntity> shuffledQuestions = new ArrayList<>(userSecurityQuestion.stream()
                .map(IamUserSecurityQuestionEntity::getSecurityQuestion)
                .toList());
        Collections.shuffle(shuffledQuestions);

        //set security questions in ForgotPasswordIdentifierResponse questions, SecurityQuestionDto
        List<SecurityQuestionsResponse.SecurityQuestionDto> questionDtos = shuffledQuestions.stream()
                .map(q -> SecurityQuestionsResponse.SecurityQuestionDto.builder()
                        .id(encryptionUtil.encrypt(q.getId().toString()))
                        .question(q.getQuestion())
                        .build())
                .toList();

        return ForgotPasswordIdentifierResponse.builder()
                .flowId(UUID.fromString(session.getSessionId()))
                .questions(questionDtos)
                .securityQuestionsRequired(requirements.isSecurityQuestionsRequired())
                .securityQuestionsCount(requirements.getSecurityQuestionsCount())
                .mfaRequired(requirements.isMfaRequired())
                .publicKey(FileUtil.cleanPublicKey(spaPublicKey))
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
