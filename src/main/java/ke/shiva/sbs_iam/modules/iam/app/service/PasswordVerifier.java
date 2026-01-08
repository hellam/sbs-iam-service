package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PasswordVerifier {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final PasswordPolicyService passwordPolicyService;

    public PasswordVerifier(CustomerAuthRepository customerAuthRepo, EmployeeAuthRepository employeeAuthRepo, @Lazy PasswordPolicyService passwordPolicyService) {
        this.customerAuthRepo = customerAuthRepo;
        this.employeeAuthRepo = employeeAuthRepo;
        this.passwordPolicyService = passwordPolicyService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean verify(SessionEntity session, String rawPassword) {
        Channel channel = session.getChannel();
        IamUserEntity user = session.getIamUser();

        return switch (channel) {
            case INTERNET_BANKING, MOBILE_BANKING -> verifyCustomer(user, rawPassword, channel);
            case BACKOFFICE -> verifyEmployee(user, rawPassword, channel);
            default -> throw BaseException.channelNotAllowed("Unsupported channel user: " + channel);
        };
    }

    private boolean verifyCustomer(IamUserEntity user, String rawPassword, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);

        if (auth.getInternetLocked()) {
            if (auth.getInternetLockoutUntil() == null || auth.getInternetLockoutUntil().isAfter(OffsetDateTime.now())) {
                throw BaseException.accountLocked("Account is locked. Please try again later or contact support.");
            } else {
                // Lock has expired, so we can reset it
                auth.setInternetLocked(false);
                auth.setInternetLockoutUntil(null);
                auth.setInternetFailedAttempts((short) 0);
                customerAuthRepo.save(auth);
            }
        }


        boolean matches = HashUtil.bcryptVerify(rawPassword, auth.getInternetPasswordHash());

        if (matches) {
            auth.setInternetFailedAttempts((short) 0);
            auth.setInternetLocked(false);
            auth.setInternetLockoutUntil(null);
            customerAuthRepo.save(auth);
            return true;
        } else {
            short failedAttempts = (short) (auth.getInternetFailedAttempts() + 1);
            auth.setInternetFailedAttempts(failedAttempts);
            if (failedAttempts >= policy.getMaxFailedAttempts()) {
                auth.setInternetLocked(true);
                if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                    auth.setInternetLockoutUntil(OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes()));
                } else {
                    auth.setInternetLockoutUntil(null); // Permanent lock
                }
            }
            customerAuthRepo.save(auth);
            return false;
        }
    }

    private boolean verifyEmployee(IamUserEntity user, String rawPassword, Channel channel) {
        EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Employee credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);

        if (auth.getStaffLocked()) {
            if (auth.getStaffLockoutUntil() == null || auth.getStaffLockoutUntil().isAfter(OffsetDateTime.now())) {
                throw BaseException.accountLocked("Account is locked. Please try again later or contact support.");
            } else {
                // Lock has expired, so we can reset it
                auth.setStaffLocked(false);
                auth.setStaffLockoutUntil(null);
                auth.setStaffFailedAttempts((short) 0);
                employeeAuthRepo.save(auth);
            }
        }

        boolean matches = HashUtil.bcryptVerify(rawPassword, auth.getStaffPasswordHash());

        if (matches) {
            auth.setStaffFailedAttempts((short) 0);
            auth.setStaffLocked(false);
            auth.setStaffLockoutUntil(null);
            employeeAuthRepo.save(auth);
            return true;
        } else {
            short failedAttempts = (short) (auth.getStaffFailedAttempts() + 1);
            auth.setStaffFailedAttempts(failedAttempts);
            if (failedAttempts >= policy.getMaxFailedAttempts()) {
                auth.setStaffLocked(true);
                if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                    auth.setStaffLockoutUntil(OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes()));
                } else {
                    auth.setStaffLockoutUntil(null); // Permanent lock
                }
            }
            employeeAuthRepo.save(auth);
            return false;
        }
    }
}
