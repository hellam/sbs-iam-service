package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeEmployeeSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeEmployeesService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Backoffice Employees", description = "Backoffice employee listing")
public class BackofficeEmployeesController {

    private final BackofficeEmployeesService backofficeEmployeesService;

    @Operation(summary = "List employees", description = "Returns a paginated employee list for backoffice")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeEmployeeSummaryResponse>>> getEmployees(
            HttpServletRequest request
    ) {
        return ResponseBuilder.success("Employees retrieved", backofficeEmployeesService.getEmployees(request));
    }
}
