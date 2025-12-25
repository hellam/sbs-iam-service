package ke.shiva.sbs_iam.modules.iam.app.service;

import jakarta.security.auth.message.AuthException;
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

    public void changePassword(SessionEntity session, PasswordChangeRequest request) {

        IamUserEntity user = session.getIamUser();
        String newPassword = request.getNewPassword();
        // 1. Validate against policy
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
}
