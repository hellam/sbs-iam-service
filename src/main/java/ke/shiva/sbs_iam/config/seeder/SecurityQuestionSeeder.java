package ke.shiva.sbs_iam.config.seeder;

import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityQuestionEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Database seeder to create initial security questions in the system
 * Creates a predefined set of security questions that can be used by users
 * <p>
 * To enable this seeder, add the following to your application.yaml:
 * seeder:
 * security-question:
 * enabled: true
 */
@Slf4j
@Component
@Order(1) // Run early, before user seeders
@RequiredArgsConstructor
public class SecurityQuestionSeeder implements CommandLineRunner {

    @Value("${seeder.security-question.enabled:false}")
    private boolean seederEnabled;

    private final SecurityQuestionRepository securityQuestionRepository;

    private static final List<String> DEFAULT_QUESTIONS = List.of(
            "What is your mother's maiden name?",
            "What was the name of your first pet?",
            "What city were you born in?",
            "What is your oldest sibling's middle name?",
            "What was the name of your elementary school?",
            "What is the name of the company of your first job?"
    );

    @Override
    @Transactional
    public void run(String... args) {
        if (!seederEnabled) {
            log.debug("Security question seeder is disabled. Set 'seeder.security-question.enabled=true' to enable it.");
            return;
        }

        log.info("Starting security question seeder...");

        try {
            long existingCount = securityQuestionRepository.count();

            if (existingCount > 0) {
                log.info("Security questions already exist (count: {}). Skipping seeder.", existingCount);
                return;
            }

            int createdCount = 0;
            for (String questionText : DEFAULT_QUESTIONS) {
                SecurityQuestionEntity question = new SecurityQuestionEntity();
                question.setQuestion(questionText);
                question.setIsActive(true);
                securityQuestionRepository.save(question);
                createdCount++;
                log.debug("Created security question: {}", questionText);
            }

            log.info("Security question seeder completed successfully! Created {} questions.", createdCount);

        } catch (Exception e) {
            log.error("Error running security question seeder", e);
            throw new RuntimeException("Failed to seed security question data", e);
        }
    }
}
