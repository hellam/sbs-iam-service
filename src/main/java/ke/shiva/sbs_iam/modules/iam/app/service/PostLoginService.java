package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.SecurityQuestionsRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.SecurityQuestionsResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SecurityQuestionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityQuestionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostLoginService {

    private final LoginFlowService loginFlowService;
    private final PasswordManager passwordManager;
    private final SecurityQuestionManager questionManager;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final SecurityQuestionPolicyRepository securityQuestionPolicyRepository;
    private final EncryptionUtil encryptionUtil;

    public void changePassword(PasswordChangeRequest req, UUID flowId) {
        if (!req.getNewPassword().equals(req.getNewPasswordConfirmation())) {
            throw BaseException.badRequest("Password confirmation does not match");
        }
        SessionEntity session = loginFlowService.requireAtLeast(flowId, LoginStage.MFA_OK);

        passwordManager.changePassword(session, req);

        // Update requirement flag
        LoginRequirements reqs = loginFlowService.getRequirements(session);
        reqs.setPasswordExpired(false);
        reqs.setFirstLogin(false);

        session.getMetadata().put("requirements", reqs);
        loginFlowService.save(session);
        loginFlowService.extend(session);
    }

    @Transactional
    public void handleQuestions(SecurityQuestionsRequest req, UUID flowId) {
        SessionEntity session = loginFlowService.requireAtLeast(flowId, LoginStage.MFA_OK);
        IamUserEntity user = session.getIamUser();

        questionManager.save(user, req.getQuestions());

        LoginRequirements reqs = loginFlowService.getRequirements(session);
        reqs.setQuestionsRequired(false);

        session.getMetadata().put("requirements", reqs);
        loginFlowService.save(session);
        loginFlowService.extend(session);
    }

    /**
     * Fetch all active security questions available in the system
     * This is a public endpoint that doesn't require authentication
     * The question IDs are encrypted for security
     * The order of questions is randomized on each fetch
     *
     * @return List of active security questions with encrypted IDs in random order
     */
    public SecurityQuestionsResponse getAllSecurityQuestions(UUID flowId) {
        List<SecurityQuestionEntity> questions = securityQuestionRepository.findAllByIsActiveTrue();

        SessionEntity session = loginFlowService.requireAtLeast(flowId, LoginStage.IDENTIFIER_OK);

        SecurityQuestionPolicyEntity policy = securityQuestionPolicyRepository.findByChannel(session.getChannel());
        if (policy == null || !policy.getEnabled()) {
            throw BaseException.badRequest("Security questions are not enabled for "+session.getChannel().getDescription());
        }

        // Shuffle questions to provide dynamic ordering on each fetch
        List<SecurityQuestionEntity> shuffledQuestions = new ArrayList<>(questions);
        Collections.shuffle(shuffledQuestions);

        List<SecurityQuestionsResponse.SecurityQuestionDto> questionDtos = shuffledQuestions.stream()
                .map(q -> SecurityQuestionsResponse.SecurityQuestionDto.builder()
                        .id(encryptionUtil.encrypt(q.getId().toString()))
                        .question(q.getQuestion())
                        .build())
                .collect(Collectors.toList());

        return SecurityQuestionsResponse.builder()
                .questions(questionDtos)
                .min(policy.getMinQuestions())
                .max(policy.getMaxQuestions())
                .build();
    }
}
