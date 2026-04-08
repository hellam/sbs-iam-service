package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.HashUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordVerifier {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final PasswordManager passwordManager;
    private final AccountLockoutService accountLockoutService;

    public PasswordVerifier(CustomerAuthRepository customerAuthRepo, EmployeeAuthRepository employeeAuthRepo, @Lazy PasswordManager passwordManager, AccountLockoutService accountLockoutService) {
        this.customerAuthRepo = customerAuthRepo;
        this.employeeAuthRepo = employeeAuthRepo;
        this.passwordManager = passwordManager;
        this.accountLockoutService = accountLockoutService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean verify(SessionEntity session, String encryptedPassword) {
        return verifyWithDetails(session, encryptedPassword).authenticated();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PasswordVerificationResult verifyWithDetails(SessionEntity session, String encryptedPassword) {
        Channel channel = session.getChannel();
        IamUserEntity user = session.getIamUser();

        // Decrypt the password from the SPA
        String rawPassword = passwordManager.decryptPassword(encryptedPassword, session.getSessionId());

        return switch (channel) {
            case INTERNET_BANKING, MOBILE_BANKING -> verifyCustomer(user, rawPassword, channel);
            case BACKOFFICE -> verifyEmployee(user, rawPassword, channel);
            default -> throw BaseException.channelNotAllowed("Unsupported channel user: " + channel);
        };
    }

    private PasswordVerificationResult verifyCustomer(IamUserEntity user, String rawPassword, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        // Check and validate lockout status (automatically unlocks if expired)
        accountLockoutService.checkAndValidateCustomerLockout(user, channel);

        String passwordHash = channel == Channel.INTERNET_BANKING
                ? auth.getInternetPasswordHash()
                : auth.getMobilePinHash();

        boolean matches = HashUtil.bcryptVerify(rawPassword, passwordHash);

        if (matches) {
            // Reset lockout on successful authentication
            if (channel == Channel.INTERNET_BANKING) {
                accountLockoutService.resetCustomerInternetLockout(user);
            } else {
                accountLockoutService.resetCustomerMobileLockout(user);
            }
            return PasswordVerificationResult.success();
        } else {
            // Record failed attempt (will lock if threshold reached)
            AccountLockoutService.LockoutAttemptResult attemptResult;
            if (channel == Channel.INTERNET_BANKING) {
                attemptResult = accountLockoutService.recordCustomerInternetFailedAttempt(user, channel);
            } else {
                attemptResult = accountLockoutService.recordCustomerMobileFailedAttempt(user, channel);
            }
            return PasswordVerificationResult.failure(toFailureDetails(attemptResult));
        }
    }

    private PasswordVerificationResult verifyEmployee(IamUserEntity user, String rawPassword, Channel channel) {
        EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Employee credentials not found");
        }

        // Check and validate lockout status (automatically unlocks if expired)
        accountLockoutService.checkAndValidateEmployeeLockout(user, channel);

        boolean matches = HashUtil.bcryptVerify(rawPassword, auth.getStaffPasswordHash());

        if (matches) {
            // Reset lockout on successful authentication
            accountLockoutService.resetEmployeeLockout(user);
            return PasswordVerificationResult.success();
        } else {
            // Record failed attempt (will lock if threshold reached)
            AccountLockoutService.LockoutAttemptResult attemptResult = accountLockoutService.recordEmployeeFailedAttempt(user, channel);
            return PasswordVerificationResult.failure(toFailureDetails(attemptResult));
        }
    }

    private PasswordFailureDetails toFailureDetails(AccountLockoutService.LockoutAttemptResult result) {
        return new PasswordFailureDetails(
                result.failedAttempts(),
                result.maxFailedAttempts(),
                result.remainingAttempts(),
                result.locked(),
                result.lockoutUntil() == null ? null : result.lockoutUntil().toString()
        );
    }

    public record PasswordVerificationResult(
            boolean authenticated,
            PasswordFailureDetails failureDetails
    ) {
        static PasswordVerificationResult success() {
            return new PasswordVerificationResult(true, null);
        }

        static PasswordVerificationResult failure(PasswordFailureDetails failureDetails) {
            return new PasswordVerificationResult(false, failureDetails);
        }
    }

    public record PasswordFailureDetails(
            short failedAttempts,
            short maxFailedAttempts,
            short remainingAttempts,
            boolean accountLocked,
            String lockoutUntil
    ) {
    }
}
