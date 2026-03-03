package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationSummaryResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeOrganizationUserResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeEmployeesService;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeOrganizationsService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Tag(name = "Backoffice Organizations", description = "Backoffice organization listing")
public class BackofficeOrganizationsController {

    private final BackofficeOrganizationsService backofficeOrganizationsService;
    private final BackofficeEmployeesService backofficeEmployeesService;

    @Operation(summary = "List organizations", description = "Returns a paginated organization list for backoffice")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<BackofficeOrganizationSummaryResponse>>> getOrganizations(
            HttpServletRequest request
    ) {
        return ResponseBuilder.success("Organizations retrieved", backofficeOrganizationsService.getOrganizations(request));
    }

    @Operation(summary = "List organization users", description = "Returns users linked to organization_user for the client ID")
    @GetMapping("/{clientId}/users")
    public ResponseEntity<ApiResponse<List<BackofficeOrganizationUserResponse>>> getOrganizationUsers(
            @PathVariable String clientId
    ) {
        return ResponseBuilder.success(
                "Organization users retrieved",
                backofficeEmployeesService.getOrganizationUsersByClientId(clientId)
        );
    }
}
