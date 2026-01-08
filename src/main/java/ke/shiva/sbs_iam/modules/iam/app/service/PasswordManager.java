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
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PasswordManager {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final OrganizationUserAuthRepository orgAuthRepo;
    private final PasswordHistoryRepository historyRepo;
    private final PasswordPolicyService passwordPolicyService;
    private final java.security.PrivateKey loginPrivateKey;

    public void changePassword(SessionEntity session, PasswordChangeRequest request) {

        IamUserEntity user = session.getIamUser();

        // Decrypt new password for validation
        String newPassword = decryptPassword(request.getNewPassword());

        // 1. Validate against policy (oldPassword will be decrypted in passwordVerifier.verify)
        passwordPolicyService.validatePasswordChange(session, request.getOldPassword(), newPassword);

        // 2. Hash new password
        String hash = HashUtil.bcrypt(newPassword);
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

    public String decryptPassword(String encryptedPassword) {
        try {
            if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
                throw BaseException.badRequest("Password is required and cannot be empty");
            }

            return TransitPasswordCrypto.decryptPayload(encryptedPassword, loginPrivateKey);
        } catch (IllegalArgumentException e) {
            // Already has good error message from TransitPasswordCrypto
            throw BaseException.badRequest(e.getMessage());
        } catch (Exception e) {
            throw BaseException.failedToDecryptPassword(
                "Failed to decrypt password. Please ensure: " +
                "1) Password was encrypted with the public key from /identify endpoint, " +
                "2) The encrypted value is properly base64 encoded, " +
                "3) No extra whitespace or characters were added. " +
                "Error: " + e.getMessage()
            );
        }
    }
}
