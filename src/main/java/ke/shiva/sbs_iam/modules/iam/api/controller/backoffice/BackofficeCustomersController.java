package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeCustomerSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeCustomersService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Backoffice Customers", description = "Backoffice customer listing")
public class BackofficeCustomersController {

    private final BackofficeCustomersService backofficeCustomersService;

    @Operation(summary = "List customers", description = "Returns a paginated customer list for backoffice")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeCustomerSummaryResponse>>> getCustomers(
            HttpServletRequest request
    ) {
        return ResponseBuilder.success("Customers retrieved", backofficeCustomersService.getCustomers(request));
    }
}
