package ke.shiva.sbs_iam.modules.iam.domain.enums.backoffice;

import java.util.Locale;

public enum BackofficeLookupType {
    CUSTOMERS("Customer"),
    ORGANIZATIONS("Organization"),
    EMPLOYEES("Employee");

    private final String label;

    BackofficeLookupType(String label) {
        this.label = label;
    }

    public String successMessage() {
        return label + " lookup successful";
    }
}
