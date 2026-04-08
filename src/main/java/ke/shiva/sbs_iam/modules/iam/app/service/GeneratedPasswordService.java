package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.util.PasswordGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratedPasswordService {

    private static final int DEFAULT_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 30;

    private final PasswordPolicyService passwordPolicyService;

    @Value("${shiva.auth.generated-password.mode:NUMERIC}")
    private String generatedPasswordMode;

    @Value("${shiva.auth.generated-password.length:8}")
    private int generatedPasswordLength;

    public String generateTemporaryPassword(Channel channel, Integer preferredLength) {
        PasswordPolicyEntity policy = channel == null ? null : passwordPolicyService.resolvePolicy(channel);
        PasswordMode mode = resolveMode(generatedPasswordMode);
        int length = resolveLength(policy, preferredLength);

        if (policy == null) {
            return generate(mode, length);
        }

        if (mode == PasswordMode.NUMERIC && requiresLettersOrSymbols(policy)) {
            log.debug("Skipping structure validation for NUMERIC generated password on channel={} because policy requires letters/symbols", channel);
            return generate(mode, length);
        }

        String generated = null;
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            generated = generate(mode, length);
            if (isStructureValid(generated, policy)) {
                return generated;
            }
        }

        log.warn("Generated temporary password did not satisfy structure after {} attempts for channel={}. Returning last generated value.", MAX_GENERATION_ATTEMPTS, channel);
        return generated == null ? generate(mode, length) : generated;
    }

    private int resolveLength(PasswordPolicyEntity policy, Integer preferredLength) {
        int length = preferredLength != null && preferredLength > 0
                ? preferredLength
                : generatedPasswordLength;
        if (length <= 0) {
            length = DEFAULT_LENGTH;
        }

        if (policy == null) {
            return length;
        }

        if (policy.getMinLength() != null && policy.getMinLength() > 0 && length < policy.getMinLength()) {
            length = policy.getMinLength();
        }
        if (policy.getMaxLength() != null && policy.getMaxLength() > 0 && length > policy.getMaxLength()) {
            length = policy.getMaxLength();
        }
        return length;
    }

    private boolean isStructureValid(String candidate, PasswordPolicyEntity policy) {
        try {
            passwordPolicyService.validateStructure(candidate, policy);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean requiresLettersOrSymbols(PasswordPolicyEntity policy) {
        return Boolean.TRUE.equals(policy.getRequireUppercase())
                || Boolean.TRUE.equals(policy.getRequireLowercase())
                || Boolean.TRUE.equals(policy.getRequireSymbol());
    }

    private String generate(PasswordMode mode, int length) {
        return switch (mode) {
            case NUMERIC -> PasswordGeneratorUtil.generateNumericPassword(length);
            case ALPHANUMERIC -> PasswordGeneratorUtil.generateRandomPassword(length);
        };
    }

    private PasswordMode resolveMode(String rawMode) {
        String normalized = rawMode == null ? "" : rawMode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALPHANUMERIC", "ALPHA", "ALPHA_NUMERIC", "RANDOM" -> PasswordMode.ALPHANUMERIC;
            case "NUMERIC", "NUMBER", "DIGITS", "DIGIT", "" -> PasswordMode.NUMERIC;
            default -> {
                log.warn("Unsupported generated password mode '{}'. Falling back to NUMERIC.", rawMode);
                yield PasswordMode.NUMERIC;
            }
        };
    }

    private enum PasswordMode {
        NUMERIC,
        ALPHANUMERIC
    }
}
