package ke.shiva.sbs_iam.modules.iam.api.controller.external;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ke.shiva.sbs_iam.modules.iam.api.response.SessionPolicyResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeSecuritySettingsService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/settings/security")
@Tag(name = "Security Settings")
public class ExtSecuritySettingsController {

    private final BackofficeSecuritySettingsService backofficeSecuritySettingsService;

    @GetMapping("/session-policy/{channel}")
    @Operation(summary = "Get session policy for channel")
    public ResponseEntity<ApiResponse<SessionPolicyResponse>> getSessionPolicy(
            @PathVariable Channel channel
    ) {
        return ResponseBuilder.success(backofficeSecuritySettingsService.getSessionPolicy(channel));
    }
}
