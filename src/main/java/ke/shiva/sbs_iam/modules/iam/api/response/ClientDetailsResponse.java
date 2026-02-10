package ke.shiva.sbs_iam.modules.iam.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Standard client/customer details response across all core banking providers.
 * Each provider maps their specific response to this standard format.
 * Only non-null fields are included in JSON responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientDetailsResponse {

    // ==================== CLIENT INFORMATION ====================

    /**
     * Client/Customer ID
     */
    private String clientId;

    /**
     * Cliennt first name
     */
    private String firstName;

    /**
     * Client middle name(s)
     */
    private String middleName;

    /**
        * Client last name
     */
    private String lastName;

    /**
     * Client full name
     */
    private String fullName;

    /**
     * Client type ID (e.g., I=Individual, C=Corporate, E=Employee)
     */
    private String clientTypeId;

    /**
     * Client status ID (e.g., A=Active, D=Dormant, C=Closed)
     */
    private String clientStatusId;

    /**
     * Client status description (e.g., Active, Dormant, Closed)
     */
    private String clientStatus;

    // ==================== CONTACT INFORMATION ====================

    /**
     * Mobile phone number
     */
    private String mobile;

    /**
     * Email address
     */
    private String email;

    // ==================== ADDRESS INFORMATION ====================

    /**
     * Address type ID (e.g., R=Residential, B=Business)
     */
    private String addressTypeId;

    /**
     * Address line 1
     */
    private String address1;

    /**
     * City ID/code
     */
    private String cityId;

    /**
     * City name
     */
    private String city;

    /**
     * Country ID/code (e.g., SO, KE, US)
     */
    private String countryId;

    /**
     * Country name
     */
    private String countryName;

    // ==================== IDENTIFICATION ====================

    /**
     * National ID or primary identification number
     */
    private String nationalId;

    /**
     * Alternative ID (e.g., Passport)
     */
    private String alternativeId;

    // ==================== MEDIA/DOCUMENTS ====================

    /**
     * Photo ID reference
     */
    private String photoId;

    /**
     * Signature ID reference
     */
    private String signId;

    // ==================== PREFERENCES ====================

    /**
     * Can send greetings (true/false)
     */
    private String canSendGreetings;

    /**
     * Can send special offers (true/false)
     */
    private String canSendOurSpecialOffers;

    /**
     * Can send associate special offers (true/false)
     */
    private String canSendAssociateSpecialOffer;

    /**
     * E-statement required (true/false)
     */
    private String eStatementRequired;

    /**
     * Mobile alert required (true/false)
     */
    private String mobileAlertRequired;

    // ==================== BUSINESS INFORMATION ====================

    /**
     * Number of employees (for corporate clients)
     */
    private String numberOfEmployees;

    /**
     * Total limits assigned to client
     */
    private String totalLimits;

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
     * Modified by user ID
     */
    private String modifiedBy;

    /**
     * Modification date/time
     */
    private LocalDateTime modifiedOn;

    /**
     * Supervised by user ID
     */
    private String supervisedBy;

    /**
     * Supervision date/time
     */
    private LocalDateTime supervisedOn;

    /**
     * Opened by user ID
     */
    private String openedBy;

    /**
     * Opening date/time
     */
    private LocalDateTime openedDate;

    /**
     * Update count/version
     */
    private Integer updateCount;

    // ==================== STATUS FLAGS ====================

    /**
     * Is account/client expired (true/false)
     */
    private String isExpired;


    // If full name is not provided, construct it from first and last name
   public String getFullName() {
       if (fullName != null && !fullName.trim().isEmpty()) {
           return fullName;
       }
       return Stream.of(firstName, middleName,lastName)
           .filter(Objects::nonNull)
           .map(String::trim)
           .filter(s -> !s.isEmpty())
           .collect(Collectors.joining(" "));
   }

   // If full name is set, split it into first , middle and last name (simple heuristic)
   public void setFullName(String fullName) {
       this.fullName = fullName;
       if (fullName != null && !fullName.trim().isEmpty()) {
           String[] parts = fullName.trim().split("\\s+");
           if (parts.length > 0) {
               if (this.firstName == null) {
                   this.firstName = parts[0];
               }
               // If there are more than 2 parts, treat the middle parts as middle name
               if (parts.length > 2 && this.middleName == null) {
                   this.middleName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
               }
               if (parts.length > 1 && this.lastName == null) {
                   this.lastName = parts[parts.length - 1];
               }
           }
       }
   }

}
