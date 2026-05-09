package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSessionPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.domain.entity.policy.SessionPolicyEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.sbs_iam.modules.iam.infra.repository.MfaPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.PasswordPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityQuestionPolicyRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackofficeSecuritySettingsServiceTest {

    @Mock
    private PasswordPolicyRepository passwordPolicyRepository;

    @Mock
    private MfaPolicyRepository mfaPolicyRepository;

    @Mock
    private SecurityQuestionPolicyRepository securityQuestionPolicyRepository;

    @Mock
    private SessionPolicyRepository sessionPolicyRepository;

    @InjectMocks
    private BackofficeSecuritySettingsService service;

    @Test
    void updateSessionPolicyAllowsFourMinuteIdleTimeoutAndTwoMinuteWarning() {
        SessionPolicyEntity existingPolicy = sessionPolicy(180, 60);
        BackofficeSessionPolicyUpdateRequest request = sessionPolicyRequest(240, 120);

        when(sessionPolicyRepository.findByChannel(Channel.INTERNET_BANKING)).thenReturn(existingPolicy);
        when(sessionPolicyRepository.save(any(SessionPolicyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateSessionPolicy(Channel.INTERNET_BANKING, request);

        assertThat(response.getInactivityTimeoutSeconds()).isEqualTo(240);
        assertThat(response.getWarningCountdownSeconds()).isEqualTo(120);
    }

    @Test
    void updateSessionPolicyRejectsUnsupportedIdleTimeout() {
        BackofficeSessionPolicyUpdateRequest request = sessionPolicyRequest(241, 60);

        when(sessionPolicyRepository.findByChannel(Channel.INTERNET_BANKING)).thenReturn(sessionPolicy(180, 60));

        assertThatThrownBy(() -> service.updateSessionPolicy(Channel.INTERNET_BANKING, request))
                .hasMessageContaining("inactivityTimeoutSeconds must be one of");
    }

    @Test
    void updateSessionPolicyRejectsWarningCountdownAtOrAboveIdleTimeout() {
        BackofficeSessionPolicyUpdateRequest request = sessionPolicyRequest(120, 120);

        when(sessionPolicyRepository.findByChannel(Channel.INTERNET_BANKING)).thenReturn(sessionPolicy(180, 60));

        assertThatThrownBy(() -> service.updateSessionPolicy(Channel.INTERNET_BANKING, request))
                .hasMessageContaining("warningCountdownSeconds must be less than inactivityTimeoutSeconds");
    }

    private static SessionPolicyEntity sessionPolicy(int inactivityTimeoutSeconds, int warningCountdownSeconds) {
        SessionPolicyEntity policy = new SessionPolicyEntity();
        policy.setChannel(Channel.INTERNET_BANKING);
        policy.setInactivityTimeoutSeconds(inactivityTimeoutSeconds);
        policy.setWarningCountdownSeconds(warningCountdownSeconds);
        return policy;
    }

    private static BackofficeSessionPolicyUpdateRequest sessionPolicyRequest(
            int inactivityTimeoutSeconds,
            int warningCountdownSeconds
    ) {
        BackofficeSessionPolicyUpdateRequest request = new BackofficeSessionPolicyUpdateRequest();
        request.setInactivityTimeoutSeconds(inactivityTimeoutSeconds);
        request.setWarningCountdownSeconds(warningCountdownSeconds);
        return request;
    }
}
