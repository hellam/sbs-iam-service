package ke.shiva.sbs_iam.modules.accounts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import ke.shiva.corebanking.model.CoreBankingResponse;
import ke.shiva.corebanking.service.CoreBankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for account-related operations.
 * Provides endpoints to retrieve account information from the core banking system.
 */
@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Operations", description = "Account information and balance retrieval")
public class AccountController {

    private final CoreBankingService coreBankingService;

    /**
     * Retrieves the balance for a specific account.
     *
     * @param accountNumber The account number to retrieve the balance for
     * @return ResponseEntity containing the account balance details
     */
    @Operation(summary = "Get Account Balance", description = "Retrieves the current balance for a specified account number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account balance",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CoreBankingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid account number"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<CoreBankingResponse<Map<String, Object>>> getAccountBalance(
            @Parameter(description = "The account number", required = true, example = "1234567890")
            @PathVariable String accountNumber) {

        log.info("Received request to get balance for account: {}", accountNumber);

        try {
            CoreBankingResponse<Map<String, Object>> response = coreBankingService.getAccountBalance(accountNumber);

            if (response.isStatus()) {
                log.info("Successfully retrieved balance for account: {}", accountNumber);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to retrieve balance for account: {}. Message: {}", accountNumber, response.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error retrieving account balance for account: {}", accountNumber, e);
            CoreBankingResponse<Map<String, Object>> errorResponse = CoreBankingResponse.<Map<String, Object>>builder()
                    .status(false)
                    .message("Error retrieving account balance: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves the full details for a specific account.
     *
     * @param accountNumber The account number to retrieve details for
     * @return ResponseEntity containing the account details
     */
    @Operation(summary = "Get Account Details", description = "Retrieves complete details for a specified account number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account details",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CoreBankingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid account number"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{accountNumber}")
    public ResponseEntity<CoreBankingResponse<Map<String, Object>>> getAccountDetails(
            @Parameter(description = "The account number", required = true, example = "1234567890")
            @PathVariable String accountNumber) {

        log.info("Received request to get details for account: {}", accountNumber);

        try {
            CoreBankingResponse<Map<String, Object>> response = coreBankingService.getAccountDetails(accountNumber);

            if (response.isStatus()) {
                log.info("Successfully retrieved details for account: {}", accountNumber);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to retrieve details for account: {}. Message: {}", accountNumber, response.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error retrieving account details for account: {}", accountNumber, e);
            CoreBankingResponse<Map<String, Object>> errorResponse = CoreBankingResponse.<Map<String, Object>>builder()
                    .status(false)
                    .message("Error retrieving account details: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves all accounts for a specific client.
     *
     * @param clientId The client ID to retrieve accounts for
     * @return ResponseEntity containing the list of client accounts
     */
    @Operation(summary = "Get Client Accounts", description = "Retrieves all accounts associated with a specified client ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved client accounts",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CoreBankingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid client ID"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/client/{clientId}")
    public ResponseEntity<?> getClientAccounts(
            @Parameter(description = "The client ID", required = true, example = "CLIENT123")
            @PathVariable String clientId) {

        log.info("Received request to get accounts for client: {}", clientId);

        try {
            var response = coreBankingService.getAccountsByClientId(clientId);

            if (response.isStatus()) {
                log.info("Successfully retrieved accounts for client: {}", clientId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to retrieve accounts for client: {}. Message: {}", clientId, response.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error retrieving accounts for client: {}", clientId, e);
            var errorResponse = CoreBankingResponse.builder()
                    .status(false)
                    .message("Error retrieving client accounts: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

