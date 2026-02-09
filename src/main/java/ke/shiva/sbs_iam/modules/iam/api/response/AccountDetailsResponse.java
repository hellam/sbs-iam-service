package ke.shiva.sbs_iam.modules.iam.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Standard account details response across all core banking providers.
 * Each provider maps their specific response to this standard format.
 * Only non-null fields are included in JSON responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountDetailsResponse {

    // ==================== ACCOUNT INFORMATION ====================

    /**
     * Account number/ID
     */
    private String accountNumber;

    /**
     * Account name/title
     */
    private String accountName;

    /**
     * IBAN (International Bank Account Number) if applicable
     */
    private String iban;

    /**
     * Account status (e.g., Active, Dormant, Closed)
     */
    private String status;

    /**
     * Product ID/code
     */
    private String productId;

    /**
     * Product name/description (e.g., Savings Account, Current Account)
     */
    private String productName;

    /**
     * Account class ID/code
     */
    private String accountClass;

    /**
     * Operating mode (e.g., Single, Joint, etc.)
     */
    private String operatingMode;

    /**
     * Allow overdraft (Yes/No)
     */
    private String allowOverdraft;

    /**
     * Overdraft limit (if applicable)
     */
    private String overdraftLimit;

    /**
     * Allow credit (Yes/No)
     */
    private String allowCredit;

    /**
     * Allow debit (Yes/No)
     */
    private String allowDebit;

    // ==================== CLIENT INFORMATION ====================

    /**
     * Client/Customer ID
     */
    private String clientId;

    /**
     * Client/Customer name
     */
    private String clientName;

    /**
     * Mobile phone number
     */
    private String mobile;

    /**
     * Email address
     */
    private String email;

    /**
     * URL or base64 string for client image/photo (if available)
     */
    private String image;

    /**
     * Signature (if available)
     */
    private String signature;

    // ==================== ADDRESS INFORMATION ====================

    /**
     * Address line 1
     */
    private String address1;

    /**
     * Address line 2
     */
    private String address2;

    /**
     * City ID/code
     */
    private String cityId;

    /**
     * Country code (e.g., SO, KE, US)
     */
    private String countryCode;

    /**
     * Country name
     */
    private String countryName;

    // ==================== BRANCH INFORMATION ====================

    /**
     * Branch ID/code
     */
    private String branchId;

    /**
     * Branch name
     */
    private String branchName;

    // ==================== BALANCE INFORMATION ====================

    /**
     * Ledger balance (clear balance)
     */
    private String ledgerBalance;

    /**
     * Unclear balance (pending transactions)
     */
    private String unclearBalance;

    /**
     * Available balance (funds available for withdrawal)
     */
    private String availableBalance;

    /**
     * Account currency code (e.g., USD, KES)
     */
    private String currency;

    // ==================== AUDIT INFORMATION ====================

    /**
     * Created by user ID
     */
    private String createdBy;

    /**
     * Creation date/time
     */
    private LocalDateTime createdOn;

    /**
     * Supervised by user ID
     */
    private String supervisedBy;

    /**
     * Supervision date/time
     */
    private LocalDateTime supervisedOn;

    /**
     * Update count/version
     */
    private Integer updateCount;



    /**
     * Convenience method to get ledger balance as BigDecimal
     */
    public BigDecimal getLedgerBalanceDecimal() {
        return ledgerBalance != null ? new BigDecimal(ledgerBalance) : BigDecimal.ZERO;
    }

    /**
     * Convenience method to get available balance as BigDecimal
     */
    public BigDecimal getAvailableBalanceDecimal() {
        return availableBalance != null ? new BigDecimal(availableBalance) : BigDecimal.ZERO;
    }

    /**
     * Convenience method to get unclear balance as BigDecimal
     */
    public BigDecimal getUnclearBalanceDecimal() {
        return unclearBalance != null ? new BigDecimal(unclearBalance) : BigDecimal.ZERO;
    }
}
