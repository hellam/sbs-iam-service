package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

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

    public static BackofficeLookupType fromString(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Lookup type is required.");
        }

        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CUSTOMER", "CUSTOMERS" -> CUSTOMERS;
            case "ORGANIZATION", "ORGANIZATIONS" -> ORGANIZATIONS;
            case "EMPLOYEE", "EMPLOYEES" -> EMPLOYEES;
            default -> throw new IllegalArgumentException("Unsupported lookup type.");
        };
    }
}
