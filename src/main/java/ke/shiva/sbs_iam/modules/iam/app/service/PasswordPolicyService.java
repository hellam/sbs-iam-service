package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PasswordHistoryRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PolicyRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final PolicyRepository policyRepo;
    private final PasswordHistoryRepository historyRepo;
    private final PasswordVerifier passwordVerifier;

    /**
     * Main entry point for validating a password change attempt.
     */
    public void validatePasswordChange(SessionEntity session, String oldPassword, String newPassword) {

        //Validate old password
        // 2. Verify password against correct credentials table
        boolean ok = passwordVerifier.verify(session, oldPassword);

        if (!ok) {
//            securityEventService.onLoginFailure(user, "PASSWORD_INVALID", session);
            throw BaseException.unauthorized("Invalid current password");
        }

        PasswordPolicyEntity policy = resolvePolicy(session.getChannel());

        validateStructure(newPassword, policy);
        validateAgainstHistory(session.getIamUser(), newPassword, policy);
        validateCommonPasswords(newPassword, policy);
    }

    /**
     * Load password policy based on channel.
     */
    public PasswordPolicyEntity resolvePolicy(Channel channel) {
        // 3. GLOBAL POLICY (always exists)
        PolicyEntity globalPolicy =
                policyRepo.findFirstByChannelsContains(channel.name())
                        .orElseThrow(() -> BaseException.unableToProcessRequest("GLOBAL password policy missing"));

        return globalPolicy.getPasswordPolicy();
    }

    /**
     * Check password length, uppercase, number, etc.
     */
    private void validateStructure(String password, PasswordPolicyEntity p) {

        if (p.getMinLength() != null && password.length() < p.getMinLength()) {
            throw BaseException.badRequest("Password is too short (min " + p.getMinLength() + ")");
        }

        if (p.getMaxLength() != null && password.length() > p.getMaxLength()) {
            throw BaseException.badRequest("Password is too long (max " + p.getMaxLength() + ")");
        }

        if (Boolean.TRUE.equals(p.getRequireUppercase()) && !password.matches(".*[A-Z].*")) {
            throw BaseException.badRequest("Password must contain an uppercase letter");
        }

        if (Boolean.TRUE.equals(p.getRequireLowercase()) && !password.matches(".*[a-z].*")) {
            throw BaseException.badRequest("Password must contain a lowercase letter");
        }

        if (Boolean.TRUE.equals(p.getRequireNumber()) && !password.matches(".*\\d.*")) {
            throw BaseException.badRequest("Password must contain a number");
        }

        if (Boolean.TRUE.equals(p.getRequireSymbol()) && !password.matches(".*[^a-zA-Z0-9].*")) {
            throw BaseException.badRequest("Password must contain a special character");
        }
    }

    /**
     * Prevent reusing old passwords.
     */
    private void validateAgainstHistory(IamUserEntity user, String newPassword, PasswordPolicyEntity p){

        int lastN = Optional.ofNullable(p.getPasswordHistoryCount()).orElse((short) 0);

        if (lastN <= 0) {
            return;
        }

        List<String> lastHashes =
                historyRepo.findPasswordHashesByIamUser(user, lastN);

        for (String oldHash : lastHashes) {
            if (HashUtil.bcryptVerify(newPassword, oldHash)) {
                throw BaseException.badRequest(
                        "Password cannot match any of the last " + lastN + " used passwords"
                );
            }
        }
    }

    /**
     * Optional: block common passwords list.
     */
    private void validateCommonPasswords(String password, PasswordPolicyEntity p) {
        if (Boolean.TRUE.equals(p.getBlockCommonPasswords())) {
            // You can load this from DB later. For now:
            List<String> common = List.of(
                    "password", "Password1", "123456", "qwerty",
                    "admin", "letmein", "welcome"
            );

            if (common.contains(password.toLowerCase())) {
                throw BaseException.badRequest("Password is too common and easily guessable");
            }
        }
    }

    /**
     * Password expiration logic to be called *after* login.
     */
//    public boolean isExpired(IamUserEntity user, PasswordPolicyEntity p) {
//        if (!Boolean.TRUE.equals(p.getExpirationEnabled())) {
//            return false;
//        }
//
//        if (user.getP == null) {
//            return true;
//        }
//
//        OffsetDateTime lastChange = user.getLastPasswordChangeAt();
//        OffsetDateTime expiry = lastChange.plusDays(p.getExpirationDays());
//
//        return OffsetDateTime.now().isAfter(expiry);
//    }
}
