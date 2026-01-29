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
    public void recordCustomerInternetFailedAttempt(IamUserEntity user, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);

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
    }

    /**
     * Increments failed attempts and locks account if threshold is reached for Customer (Mobile Banking).
     *
     * @param user    the IAM user
     * @param channel the channel
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCustomerMobileFailedAttempt(IamUserEntity user, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Customer credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);

        short failedAttempts = (short) (auth.getMobileFailedAttempts() + 1);
        auth.setMobileFailedAttempts(failedAttempts);

        if (failedAttempts >= policy.getMaxFailedAttempts()) {
            auth.setMobileLocked(true);
            if (policy.getLockoutMinutes() != null && policy.getLockoutMinutes() > 0) {
                auth.setMobileLockoutUntil(OffsetDateTime.now().plusMinutes(policy.getLockoutMinutes()));
            } else {
                auth.setMobileLockoutUntil(null); // Permanent lock
            }
        }

        customerAuthRepo.save(auth);
    }

    /**
     * Increments failed attempts and locks account if threshold is reached for Employee.
     *
     * @param user    the IAM user
     * @param channel the channel
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEmployeeFailedAttempt(IamUserEntity user, Channel channel) {
        EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
        if (auth == null) {
            throw BaseException.iamUserCredentialsNotFound("Employee credentials not found");
        }

        PasswordPolicyEntity policy = passwordPolicyService.resolvePolicy(channel);

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
}

