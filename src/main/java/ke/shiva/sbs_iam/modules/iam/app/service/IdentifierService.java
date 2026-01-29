package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.api.request.IdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.IdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.LoginIdentifierEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.domain.enums.user.IamStatus;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginIdentifierRepository;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentifierService {

    private final LoginIdentifierRepository identifierRepo;
    private final PolicyEvaluationService policyService;
    private final LoginFlowService loginFlowService;
    private final LoginHistoryService loginHistoryService;
    private final CustomerAuthRepository customerAuthRepo;
    private final EmployeeAuthRepository employeeAuthRepo;

    @Value("${shiva.security.spa.public-key}")
    private String spaPublicKey;

    @Transactional
    public IdentifierResponse handle(IdentifierRequest req, String deviceId) {
        //Verify deviceId exists if provided
//        loginFlowService.verifyDeviceId(deviceId);

        Channel channel = req.getChannel();

        LoginIdentifierEntity identifier = identifierRepo
                .findByIdentifierAndChannelAndStatus(req.getIdentifier(), channel, IamStatus.ACTIVE)
                .orElseThrow(() -> {
                    // Log failed identifier verification
                    loginHistoryService.logIdentifierFailure(
                            req.getIdentifier(),
                            channel.name(),
                            "IDENTIFIER_NOT_FOUND"
                    );
                    return BaseException.unauthorized("Invalid credentials");
                });

        IamUserEntity user = identifier.getIamUser();

        if (user.getStatus() != IamStatus.ACTIVE) {
            // Log failed identifier verification due to inactive user
            loginHistoryService.logIdentifierFailure(
                    req.getIdentifier(),
                    channel.name(),
                    "USER_INACTIVE"
            );
            throw BaseException.unauthorized("Invalid credentials");
        }

        // Check if account is locked before proceeding
        checkAccountLockout(user, channel, req.getIdentifier());

        // evaluate policy requirements
        LoginRequirements requirements = policyService.evaluateLoginRequirements(user, channel);

        // create temp session (flow)
        var session = loginFlowService.start(user, channel, requirements, req.getIdentifier(), deviceId);

        // Log successful identifier verification
        loginHistoryService.logIdentifierSuccess(user, req.getIdentifier(), session);

        IdentifierResponse resp = new IdentifierResponse();
        resp.setFlowId(UUID.fromString(session.getSessionId()));
        resp.setPasswordRequired(true);
        resp.setOtpRequired(requirements.isOtpRequired());
        resp.setTotpRequired(requirements.isTotpRequired());
        resp.setPasswordExpired(requirements.isPasswordExpired());
        resp.setFirstLogin(requirements.isFirstLogin());
        resp.setSecurityQuestionsRequired(requirements.isQuestionsRequired());
        resp.setPublicKey(FileUtil.cleanPublicKey(spaPublicKey));
        resp.setProfileSelectionRequired(requirements.isProfileSelectionRequired());

        return resp;
    }

    private void checkAccountLockout(IamUserEntity user, Channel channel, String identifier) {
        switch (channel) {
            case INTERNET_BANKING, MOBILE_BANKING -> checkCustomerLockout(user, identifier, channel);
            case BACKOFFICE -> checkEmployeeLockout(user, identifier, channel);
        }
    }

    private void checkCustomerLockout(IamUserEntity user, String identifier, Channel channel) {
        CustomerAuthEntity auth = customerAuthRepo.findByIamUser(user);
        if (auth == null) {
            return; // No auth record means no lockout
        }

        if (auth.getInternetLocked()) {
            if (auth.getInternetLockoutUntil() == null || auth.getInternetLockoutUntil().isAfter(OffsetDateTime.now())) {
                // Log failed attempt due to account being locked
                loginHistoryService.logIdentifierFailure(
                        identifier,
                        channel.name(),
                        "ACCOUNT_LOCKED"
                );
                throw BaseException.accountLocked("Account is locked. Please try again later or contact support.");
            } else {
                // Lock has expired, so we can reset it
                auth.setInternetLocked(false);
                auth.setInternetLockoutUntil(null);
                auth.setInternetFailedAttempts((short) 0);
                customerAuthRepo.save(auth);
            }
        }
    }

    private void checkEmployeeLockout(IamUserEntity user, String identifier, Channel channel) {
        EmployeeAuthEntity auth = employeeAuthRepo.findByIamUser(user);
        if (auth == null) {
            return; // No auth record means no lockout
        }

        if (auth.getStaffLocked()) {
            if (auth.getStaffLockoutUntil() == null || auth.getStaffLockoutUntil().isAfter(OffsetDateTime.now())) {
                // Log failed attempt due to account being locked
                loginHistoryService.logIdentifierFailure(
                        identifier,
                        channel.name(),
                        "ACCOUNT_LOCKED"
                );
                throw BaseException.accountLocked("Account is locked. Please try again later or contact support.");
            } else {
                // Lock has expired, so we can reset it
                auth.setStaffLocked(false);
                auth.setStaffLockoutUntil(null);
                auth.setStaffFailedAttempts((short) 0);
                employeeAuthRepo.save(auth);
            }
        }
    }
}