package ke.shiva.sbs_iam.modules.iam.app.service;

import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.CustomerAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.auth.EmployeeAuthEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.MfaPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.CustomerAuthRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.EmployeeAuthRepository;
import ke.shiva.shivacorestarter.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpVerifierTest {

    @Mock
    private CustomerAuthRepository customerAuthRepository;

    @Mock
    private EmployeeAuthRepository employeeAuthRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private EncryptionUtil encryptionUtil;

    private TotpVerifier totpVerifier;

    @BeforeEach
    void setUp() {
        totpVerifier = new TotpVerifier(
                customerAuthRepository,
                employeeAuthRepository,
                policyService,
                encryptionUtil
        );

        ReflectionTestUtils.setField(totpVerifier, "timeStepSeconds", 30);
        ReflectionTestUtils.setField(totpVerifier, "allowedPastWindows", 1);
        ReflectionTestUtils.setField(totpVerifier, "allowedFutureWindows", 0);
        ReflectionTestUtils.setField(totpVerifier, "secretBytes", 20);

        MfaPolicyEntity policy = new MfaPolicyEntity();
        policy.setOtpLength((short) 6);
        when(policyService.getMfaPolicy(Channel.INTERNET_BANKING)).thenReturn(policy);
        when(policyService.getMfaPolicy(Channel.BACKOFFICE)).thenReturn(policy);
        when(policyService.getMfaPolicy(Channel.MOBILE_BANKING)).thenReturn(policy);
    }

    @Test
    void verifySecret_acceptsCurrentWindowCode() {
        String secret = totpVerifier.generateSecret();
        long currentEpoch = 1_700_000_200L;
        String code = totpVerifier.generateCodeForEpochSeconds(secret, currentEpoch, 6);

        assertTrue(totpVerifier.verifySecretAtEpochSeconds(secret, code, Channel.INTERNET_BANKING, currentEpoch));
    }

    @Test
    void verifySecret_acceptsPreviousWindowCode() {
        String secret = totpVerifier.generateSecret();
        long currentEpoch = 1_700_000_200L;
        long previousEpoch = currentEpoch - 30;
        String previousWindowCode = totpVerifier.generateCodeForEpochSeconds(secret, previousEpoch, 6);

        assertTrue(totpVerifier.verifySecretAtEpochSeconds(secret, previousWindowCode, Channel.INTERNET_BANKING, currentEpoch));
    }

    @Test
    void verifySecret_rejectsCodeOutsideGraceWindow() {
        String secret = totpVerifier.generateSecret();
        long currentEpoch = 1_700_000_200L;
        long staleEpoch = currentEpoch - 60;
        String staleCode = totpVerifier.generateCodeForEpochSeconds(secret, staleEpoch, 6);

        assertFalse(totpVerifier.verifySecretAtEpochSeconds(secret, staleCode, Channel.INTERNET_BANKING, currentEpoch));
    }

    @Test
    void verify_backofficeUsesEmployeeSecretWhenUserHasBoth() {
        IamUserEntity user = new IamUserEntity();

        String customerSecret = totpVerifier.generateSecret();
        String employeeSecret = totpVerifier.generateSecret();

        CustomerAuthEntity customerAuth = new CustomerAuthEntity();
        customerAuth.setMfaEnabled(true);
        customerAuth.setMfaSecret(customerSecret);

        EmployeeAuthEntity employeeAuth = new EmployeeAuthEntity();
        employeeAuth.setMfaEnabled(true);
        employeeAuth.setMfaSecret(employeeSecret);

        when(customerAuthRepository.findByIamUser(user)).thenReturn(customerAuth);
        when(employeeAuthRepository.findByIamUser(user)).thenReturn(employeeAuth);

        long now = System.currentTimeMillis() / 1000;
        String employeeCode = totpVerifier.generateCodeForEpochSeconds(employeeSecret, now, 6);
        String customerCode = totpVerifier.generateCodeForEpochSeconds(customerSecret, now, 6);
        int attempts = 0;
        while (customerCode.equals(employeeCode) && attempts++ < 5) {
            employeeSecret = totpVerifier.generateSecret();
            employeeAuth.setMfaSecret(employeeSecret);
            employeeCode = totpVerifier.generateCodeForEpochSeconds(employeeSecret, now, 6);
        }

        assertTrue(totpVerifier.verify(user, employeeCode, Channel.BACKOFFICE));
        assertFalse(totpVerifier.verify(user, customerCode, Channel.BACKOFFICE));
    }

    @Test
    void verify_internetBankingUsesCustomerSecretWhenUserHasBoth() {
        IamUserEntity user = new IamUserEntity();

        String customerSecret = totpVerifier.generateSecret();
        String employeeSecret = totpVerifier.generateSecret();

        CustomerAuthEntity customerAuth = new CustomerAuthEntity();
        customerAuth.setMfaEnabled(true);
        customerAuth.setMfaSecret(customerSecret);

        EmployeeAuthEntity employeeAuth = new EmployeeAuthEntity();
        employeeAuth.setMfaEnabled(true);
        employeeAuth.setMfaSecret(employeeSecret);

        when(customerAuthRepository.findByIamUser(user)).thenReturn(customerAuth);
        when(employeeAuthRepository.findByIamUser(user)).thenReturn(employeeAuth);

        long now = System.currentTimeMillis() / 1000;
        String customerCode = totpVerifier.generateCodeForEpochSeconds(customerSecret, now, 6);
        String employeeCode = totpVerifier.generateCodeForEpochSeconds(employeeSecret, now, 6);
        int attempts = 0;
        while (customerCode.equals(employeeCode) && attempts++ < 5) {
            customerSecret = totpVerifier.generateSecret();
            customerAuth.setMfaSecret(customerSecret);
            customerCode = totpVerifier.generateCodeForEpochSeconds(customerSecret, now, 6);
        }

        assertTrue(totpVerifier.verify(user, customerCode, Channel.INTERNET_BANKING));
        assertFalse(totpVerifier.verify(user, employeeCode, Channel.INTERNET_BANKING));
    }
}
