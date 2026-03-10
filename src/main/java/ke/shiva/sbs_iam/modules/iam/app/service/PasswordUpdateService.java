package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.PasswordHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PasswordHistoryRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordUpdateService {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final PasswordHistoryRepository historyRepo;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * Update user's password for the given channel
     * Used for forgot password and admin password resets
     */
    public void updatePassword(IamUserEntity user, String plainPassword, Channel channel) {
        updatePassword(user, plainPassword, channel, false);
    }

    /**
     * Update user's password and optionally force first-login password reset.
     */
    public void updatePassword(IamUserEntity user, String plainPassword, Channel channel, boolean forceFirstLogin) {
        // Hash new password
        String hash = HashUtil.bcrypt(plainPassword);

        // Calculate expiry based on policy
        PasswordPolicyEntity passwordPolicy = passwordPolicyService.resolvePolicy(channel);
        OffsetDateTime expiry = null;
        if (passwordPolicy.getExpirationEnabled() && passwordPolicy.getExpirationDays() > 0) {
            expiry = OffsetDateTime.now().plusDays(passwordPolicy.getExpirationDays());
        }

        // Update correct credentials table based on channel
        switch (channel) {
            case INTERNET_BANKING, MOBILE_BANKING -> {
                CustomerAuthEntity auth = customerAuthRepo.findByIamUserId(user.getId())
                        .orElseThrow(() -> BaseException.channelNotAllowed("CustomerAuth missing"));
                if (channel == Channel.MOBILE_BANKING) {
                    auth.setMobilePinHash(hash);
                    auth.setMobilePinSetAt(OffsetDateTime.now());
                    auth.setMobileFirstTimeLogin(forceFirstLogin);
                } else {
                    auth.setInternetPasswordHash(hash);
                    auth.setInternetPasswordChangedAt(OffsetDateTime.now());
                    auth.setInternetPasswordExpiry(expiry);
                    auth.setInternetFirstTimeLogin(forceFirstLogin);
                }
                customerAuthRepo.save(auth);
            }

            case BACKOFFICE -> {
                EmployeeAuthEntity auth = employeeAuthRepo.findByIamUserId(user.getId())
                        .orElseThrow(() -> BaseException.channelNotAllowed("EmployeeAuth missing"));
                auth.setStaffPasswordHash(hash);
                auth.setStaffPasswordExpiry(expiry);
                auth.setFirstTimeLogin(forceFirstLogin);
                employeeAuthRepo.save(auth);
            }

            default -> throw BaseException.channelNotAllowed("Unsupported channel: " + channel);
        }

        // Save to password history
        PasswordHistoryEntity history = new PasswordHistoryEntity();
        history.setIamUser(user);
        history.setPasswordHash(hash);
        history.setCreatedAt(OffsetDateTime.now());
        historyRepo.save(history);

        log.info("Password updated successfully for user: {} on channel: {}, forceFirstLogin={}",
                user.getPublicId(), channel, forceFirstLogin);
    }
}
