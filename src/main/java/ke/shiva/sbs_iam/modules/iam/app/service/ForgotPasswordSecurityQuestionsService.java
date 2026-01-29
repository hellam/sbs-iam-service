package ke.shiva.sbs_iam.modules.iam.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.shiva.sbs_iam.modules.iam.api.request.ForgotPasswordSecurityQuestionsRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.ForgotPasswordSecurityQuestionsResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.IamUserSecurityQuestionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.ForgotPasswordRequirements;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordSecurityQuestionsService {

    private final ForgotPasswordFlowService flowService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ForgotPasswordSecurityQuestionsResponse handle(ForgotPasswordSecurityQuestionsRequest request, UUID flowId) {
        // Get and validate session
        SessionEntity session = flowService.requireStage(flowId, LoginStage.FP_IDENTIFIER_OK);

        // Get requirements from metadata
        ForgotPasswordRequirements requirements = objectMapper.convertValue(
                session.getMetadata().get("requirements"),
                ForgotPasswordRequirements.class
        );

        if (!requirements.isSecurityQuestionsRequired()) {
            log.warn("Security questions not required but attempted for session: {}", session.getSessionId());
            throw BaseException.badRequest();
        }

        // Get user's security questions
        IamUserEntity user = session.getIamUser();
        List<IamUserSecurityQuestionEntity> userQuestions = user.getIamUserSecurityQuestions().stream().toList();

        if (userQuestions.isEmpty()) {
            throw BaseException.badRequest("No security questions set up for this account");
        }

        // Validate answers
        Map<Long, String> providedAnswers = request.getAnswers().stream()
                .collect(Collectors.toMap(
                        ForgotPasswordSecurityQuestionsRequest.SecurityQuestionAnswer::getQuestionId,
                        ForgotPasswordSecurityQuestionsRequest.SecurityQuestionAnswer::getAnswer
                ));

        int correctAnswers = 0;
        int requiredCorrectAnswers = requirements.getSecurityQuestionsCount();

        for (IamUserSecurityQuestionEntity userQuestion : userQuestions) {
            Long questionId = userQuestion.getSecurityQuestion().getId();
            String providedAnswer = providedAnswers.get(questionId);

            if (providedAnswer != null) {
                // Compare hashed answers
                if (HashUtil.bcryptVerify(providedAnswer.trim().toLowerCase(), userQuestion.getAnswerHash())) {
                    correctAnswers++;
                }
            }
        }

        if (correctAnswers < requiredCorrectAnswers) {
            log.warn("Failed security questions verification for session: {}", session.getSessionId());
            //TODO: Implement account lockout after multiple failed attempts
            throw BaseException.unauthorized("Security questions verification failed. Please try again.");
        }

        // Update stage
        flowService.updateStage(session, LoginStage.FP_SECURITY_QUESTIONS_OK);

        // Determine next step
        String nextStep = requirements.isMfaRequired() ? "MFA" : "RESET_PASSWORD";

        return ForgotPasswordSecurityQuestionsResponse.builder()
                .verified(true)
                .nextStep(nextStep)
                .build();
    }
}
