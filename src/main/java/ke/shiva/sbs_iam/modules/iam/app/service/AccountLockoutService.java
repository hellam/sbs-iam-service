package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.PasswordPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Dynamic account lockout service that handles both Customer and Employee authentication
 * entities based on channel and policy configuration.
 */
@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * Checks and validates account lockout status for Customer (Internet/Mobile Banking).
     * Automatically unlocks if lockout period has expired.
     *
     * @param user    the IAM user
     * @param channel the channel (INTERNET_BANKING or MOBILE_BANKING)
     * @throws BaseException if account is locked
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndValidateCustomerLockout(IamUserEntity user, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        if (channel == Channel.INTERNET_BANKING) {
            checkLockout(
                    auth.getInternetLocked(),
                    auth.getInternetLockoutUntil(),
                    () -> resetCustomerInternetLock(auth)
            );
        } else if (channel == Channel.MOBILE_BANKING) {
            checkLockout(
                    auth.getMobileLocked(),
                    auth.getMobileLockoutUntil(),
                    () -> resetCustomerMobileLock(auth)
            );
        }
    }

    /**
     * Checks and validates account lockout status for Employee (Backoffice).
     * Automatically unlocks if lockout period has expired.
     *
     * @param user    the IAM user
     * @param channel the channel (BACKOFFICE)
     * @throws BaseException if account is locked
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndValidateEmployeeLockout(IamUserEntity user, Channel channel) {
        EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Employee credentials not found");
        }

        checkLockout(
                auth.getStaffLocked(),
                auth.getStaffLockoutUntil(),
                () -> resetEmployeeStaffLock(auth)
        );
    }

    /**
     * Increments failed attempts and locks account if threshold is reached for Customer (Internet Banking).
     *
     * @param user    the IAM user
     * @param channel the channel
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LockoutAttemptResult recordCustomerInternetFailedAttempt(IamUserEntity user, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);
        if (policy == null) {
            throw BaseException.unableToProcessRequest("No password policy configured for channel: " + channel);
        }

        short currentAttempts = auth.getInternetFailedAttempts() == null ? 0 : auth.getInternetFailedAttempts();
        short maxFailedAttempts = resolveMaxFailedAttempts(policy);
        short failedAttempts = (short) (currentAttempts + 1);
        auth.setInternetFailedAttempts(failedAttempts);

        OffsetDateTime lockoutUntil = null;
        boolean locked = false;
        if (failedAttempts >= maxFailedAttempts) {
            auth.setInternetLocked(true);
            locked = true;
            if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                lockoutUntil = OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes());
                auth.setInternetLockoutUntil(lockoutUntil);
            } else {
                auth.setInternetLockoutUntil(null); // Permanent lock
            }
        }

        customerAuthRepo.save(auth);
        short remainingAttempts = (short) Math.max(0, maxFailedAttempts - failedAttempts);
        return new LockoutAttemptResult(failedAttempts, maxFailedAttempts, remainingAttempts, locked, lockoutUntil);
    }

    /**
     * Increments failed attempts and locks account if threshold is reached for Customer (Mobile Banking).
     *
     * @param user    the IAM user
     * @param channel the channel
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LockoutAttemptResult recordCustomerMobileFailedAttempt(IamUserEntity user, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);
        if (policy == null) {
            throw BaseException.unableToProcessRequest("No password policy configured for channel: " + channel);
        }

        short currentAttempts = auth.getMobileFailedAttempts() == null ? 0 : auth.getMobileFailedAttempts();
        short maxFailedAttempts = resolveMaxFailedAttempts(policy);
        short failedAttempts = (short) (currentAttempts + 1);
        auth.setMobileFailedAttempts(failedAttempts);

        OffsetDateTime lockoutUntil = null;
        boolean locked = false;
        if (failedAttempts >= maxFailedAttempts) {
            auth.setMobileLocked(true);
            locked = true;
            if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                lockoutUntil = OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes());
                auth.setMobileLockoutUntil(lockoutUntil);
            } else {
                auth.setMobileLockoutUntil(null); // Permanent lock
            }
        }

        customerAuthRepo.save(auth);
        short remainingAttempts = (short) Math.max(0, maxFailedAttempts - failedAttempts);
        return new LockoutAttemptResult(failedAttempts, maxFailedAttempts, remainingAttempts, locked, lockoutUntil);
    }

    /**
     * Increments failed attempts and locks account if threshold is reached for Employee.
     *
     * @param user    the IAM user
     * @param channel the channel
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LockoutAttemptResult recordEmployeeFailedAttempt(IamUserEntity user, Channel channel) {
        EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Employee credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);
        if (policy == null) {
            throw BaseException.unableToProcessRequest("No password policy configured for channel: " + channel);
        }

        short currentAttempts = auth.getStaffFailedAttempts() == null ? 0 : auth.getStaffFailedAttempts();
        short maxFailedAttempts = resolveMaxFailedAttempts(policy);
        short failedAttempts = (short) (currentAttempts + 1);
        auth.setStaffFailedAttempts(failedAttempts);

        OffsetDateTime lockoutUntil = null;
        boolean locked = false;
        if (failedAttempts >= maxFailedAttempts) {
            auth.setStaffLocked(true);
            locked = true;
            if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                lockoutUntil = OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes());
                auth.setStaffLockoutUntil(lockoutUntil);
            } else {
                auth.setStaffLockoutUntil(null); // Permanent lock
            }
        }

        employeeAuthRepo.save(auth);
        short remainingAttempts = (short) Math.max(0, maxFailedAttempts - failedAttempts);
        return new LockoutAttemptResult(failedAttempts, maxFailedAttempts, remainingAttempts, locked, lockoutUntil);
    }

    /**
     * Resets failed attempts and lockout status for Customer (Internet Banking).
     *
     * @param user the IAM user
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetCustomerInternetLockout(IamUserEntity user) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth != null) {
            resetCustomerInternetLock(auth);
        }
    }

    /**
     * Resets failed attempts and lockout status for Customer (Mobile Banking).
     *
     * @param user the IAM user
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetCustomerMobileLockout(IamUserEntity user) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth != null) {
            resetCustomerMobileLock(auth);
        }
    }

    /**
     * Resets failed attempts and lockout status for Employee.
     *
     * @param user the IAM user
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetEmployeeLockout(IamUserEntity user) {
        EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
        if (auth != null) {
            resetEmployeeStaffLock(auth);
        }
    }

    // Private helper methods

    /**
     * Generic lockout checker that validates if an account is locked and handles automatic unlock.
     *
     * @param isLocked      whether the account is currently locked
     * @param lockoutUntil  the lockout expiry time (null = permanent)
     * @param resetCallback callback to reset the lock if expired
     * @throws BaseException if account is still locked
     */
    private void checkLockout(Boolean isLocked, OffsetDateTime lockoutUntil, Runnable resetCallback) {
        if (Boolean.TRUE.equals(isLocked)) {
            if (lockoutUntil == null || lockoutUntil.isAfter(OffsetDateTime.now())) {
                // Account is locked and lock hasn't expired
                String message = lockoutUntil == null
                        ? "Account is permanently locked. Please contact support."
                        : "Account is locked. Please try again later or contact support.";
                throw BaseException.accountLocked(message);
            } else {
                // Lock has expired, reset it
                resetCallback.run();
            }
        }
    }

    private void resetCustomerInternetLock(CustomerAuthEntity auth) {
        auth.setInternetLocked(false);
        auth.setInternetLockoutUntil(null);
        auth.setInternetFailedAttempts((short) 0);
        customerAuthRepo.save(auth);
    }

    private void resetCustomerMobileLock(CustomerAuthEntity auth) {
        auth.setMobileLocked(false);
        auth.setMobileLockoutUntil(null);
        auth.setMobileFailedAttempts((short) 0);
        customerAuthRepo.save(auth);
    }

    private void resetEmployeeStaffLock(EmployeeAuthEntity auth) {
        auth.setStaffLocked(false);
        auth.setStaffLockoutUntil(null);
        auth.setStaffFailedAttempts((short) 0);
        employeeAuthRepo.save(auth);
    }

    private short resolveMaxFailedAttempts(PasswordPolicyEntity policy) {
        if (policy.getMaxFailedAttempts() == null || policy.getMaxFailedAttempts() <= 0) {
            return 5;
        }
        return policy.getMaxFailedAttempts();
    }

    /**
     * Generic method to handle OTP verification lockout based on channel.
     * Can be used by OtpService to lock accounts after too many failed OTP attempts.
     *
     * @param user    the IAM user
     * @param channel the channel
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockAccountAttemptFailure(IamUserEntity user, Channel channel) {
        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);
        if (policy == null) {
            throw BaseException.unableToProcessRequest("No password policy configured for channel: " + channel);
        }

        switch (channel) {
            case INTERNET_BANKING -> {
                CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
                if (auth != null) {
                    auth.setInternetLocked(true);
                    if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                        auth.setInternetLockoutUntil(OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes()));
                    } else {
                        auth.setInternetLockoutUntil(null);
                    }
                    customerAuthRepo.save(auth);
                }
            }
            case MOBILE_BANKING -> {
                CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
                if (auth != null) {
                    auth.setMobileLocked(true);
                    if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                        auth.setMobileLockoutUntil(OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes()));
                    } else {
                        auth.setMobileLockoutUntil(null);
                    }
                    customerAuthRepo.save(auth);
                }
            }
            case BACKOFFICE -> {
                EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
                if (auth != null) {
                    auth.setStaffLocked(true);
                    if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                        auth.setStaffLockoutUntil(OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes()));
                    } else {
                        auth.setStaffLockoutUntil(null);
                    }
                    employeeAuthRepo.save(auth);
                }
            }
        }
    }

    public record LockoutAttemptResult(
            short failedAttempts,
            short maxFailedAttempts,
            short remainingAttempts,
            boolean locked,
            OffsetDateTime lockoutUntil
    ) {
    }
}
