package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.SecurityQuestionsRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.IamUserSecurityQuestionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityQuestionEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamUserSecurityQuestionRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityQuestionManager {

    private final IamUserSecurityQuestionRepository userQuestionRepo;
    private final SecurityQuestionRepository securityQuestionRepo;

    public void save(IamUserEntity user, List<SecurityQuestionsRequest.QuestionAnswer> questions) {

        // Delete previous questions if any (optional)
        userQuestionRepo.deleteAllByIamUserId(user.getId());

        for (var qa : questions) {
            SecurityQuestionEntity securityQuestionEntity = securityQuestionRepo.findById(qa.getQuestionId()).
                    orElseThrow(() -> BaseException.badRequest("Invalid security question: " + qa.getQuestionId()));

            IamUserSecurityQuestionEntity entity = new IamUserSecurityQuestionEntity();
            entity.setIamUser(user);
            entity.setSecurityQuestion(securityQuestionEntity);
            entity.setAnswerHash(HashUtil.bcrypt(qa.getAnswer()));
            entity.setCreatedAt(OffsetDateTime.now());

            userQuestionRepo.save(entity);
        }
    }
}
