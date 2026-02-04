package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.PasswordHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.*;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import ke.shiva.shivacorestarter.util.TransitPasswordCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordManager {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final OrganizationUserAuthRepository orgAuthRepo;
    private final PasswordHistoryRepository historyRepo;
    private final PasswordPolicyService passwordPolicyService;
    private final java.security.PrivateKey loginPrivateKey;
    private final ke.shiva.sbs_iam.modules.iam.infra.repository.SessionRepository sessionRepository;

    public void changePassword(SessionEntity session, PasswordChangeRequest request) {

        IamUserEntity user = session.getIamUser();

        // 1. Validate against policy (oldPassword will be decrypted in passwordVerifier.verify)
        passwordPolicyService.validatePasswordChange(session, request.getOldPassword(), request.getNewPassword());

        // 2. Hash new password
        String hash = HashUtil.bcrypt(request.getNewPassword());
        PasswordPolicyEntity passwordPolicy = passwordPolicyService.resolvePolicy(session.getChannel());

        OffsetDateTime expiry = null;
        if (passwordPolicy.getExpirationEnabled() && passwordPolicy.getExpirationDays() > 0) {
            expiry = OffsetDateTime.now().plusDays(passwordPolicy.getExpirationDays());
        }

        // 3. Update correct credentials table
        switch (session.getChannel()) {

            case INTERNET_BANKING,MOBILE_BANKING -> {
                CustomerAuthEntity auth = customerAuthRepo.findByIamUserId(user.getId())
                        .orElseThrow(() -> BaseException.channelNotAllowed("CustomerAuth missing"));
                auth.setInternetPasswordHash(hash);
                auth.setInternetPasswordExpiry(expiry);
                auth.setInternetFirstTimeLogin(false);
                customerAuthRepo.save(auth);
            }

            case BACKOFFICE -> {
                EmployeeAuthEntity auth = employeeAuthRepo.findByIamUserId(user.getId())
                        .orElseThrow(() -> BaseException.channelNotAllowed("EmployeeAuth missing"));
                auth.setStaffPasswordHash(hash);
                auth.setStaffPasswordExpiry(expiry);
                auth.setFirstTimeLogin(false);
                employeeAuthRepo.save(auth);
            }

            default -> throw BaseException.channelNotAllowed("Unsupported user category");
        }
        PasswordHistoryEntity history = new PasswordHistoryEntity();
        history.setIamUser(user);
        history.setPasswordHash(hash);
        history.setCreatedAt(OffsetDateTime.now());
        historyRepo.save(history);
    }

    public String decryptPassword(String encryptedPassword, String _sessionId) {
        try {
            if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
                throw BaseException.badRequest("Password is required and cannot be empty");
            }

            String saltedPassword = TransitPasswordCrypto.decryptPayload(encryptedPassword, loginPrivateKey);

            // Extract session ID and encrypted password (format: sessionId:encryptedPassword)
            String[] parts = saltedPassword.split(":", 2);
            if (parts.length != 2) {
                throw BaseException.badRequest("Invalid password format. Expected: sessionId:encryptedPassword");
            }

            String sessionId = parts[0];
            String plainPassword = parts[1];

            if (!sessionId.equals(_sessionId)) {
                log.warn("Session ID mismatch during password decryption. Expected: {}, Received: {}", _sessionId, sessionId);
                throw BaseException.badRequest();
            }

            // Validate session exists and is active
            SessionEntity session = sessionRepository.findBySessionId(sessionId);
            if (session == null) {
                throw BaseException.unauthorized("Invalid or expired session");
            }

            if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(OffsetDateTime.now())) {
                throw BaseException.unauthorized("Session has expired");
            }

            // return decrypted password
            return plainPassword;
        } catch (IllegalArgumentException e) {
            throw BaseException.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Error during password decryption: {}", e.getMessage());
            throw BaseException.badRequest();
        }
    }
}
