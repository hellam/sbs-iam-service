package ke.shiva.sbs_iam.modules.iam.domain.enums.policy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PolicyType {
    PIN_POLICY("Pin Polcy"),
    PASSWORD_POLICY("Password Policy"),
    FEATURE_POLICY("Feature Policy"),
    SEC_QN_POLICY("Security Question Policy"),
    MFA_POLICY("MFA Policy"),
    SESSION_POLICY("Session Policy");

    private final String value;
}
