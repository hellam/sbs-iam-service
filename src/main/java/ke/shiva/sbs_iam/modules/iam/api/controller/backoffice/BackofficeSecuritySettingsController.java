package ke.shiva.sbs_iam.modules.iam.api.controller.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeMfaPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficePasswordPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSecurityQuestionPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.backoffice.BackofficeSessionPolicyUpdateRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.SessionPolicyResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeMfaPolicyDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficePasswordPolicyResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeSecurityQuestionPolicyDetailsResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeSecuritySettingsResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.backoffice.BackofficeSecuritySettingsService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings/security")
@RequiredArgsConstructor
@Tag(name = "Backoffice Security Settings", description = "Backoffice APIs for channel security policy management")
public class BackofficeSecuritySettingsController {

    private final BackofficeSecuritySettingsService backofficeSecuritySettingsService;

    @GetMapping("/{channel}")
    @Operation(summary = "Get security settings for channel")
    public ResponseEntity<ApiResponse<BackofficeSecuritySettingsResponse>> getSecuritySettings(
            @PathVariable Channel channel
    ) {
        return ResponseBuilder.success(backofficeSecuritySettingsService.getSettings(channel));
    }

    @PutMapping("/{channel}/password-policy")
    @Operation(summary = "Update password policy for channel")
    public ResponseEntity<ApiResponse<BackofficePasswordPolicyResponse>> updatePasswordPolicy(
            @PathVariable Channel channel,
            @Valid @RequestBody BackofficePasswordPolicyUpdateRequest request
    ) {
        return ResponseBuilder.success(backofficeSecuritySettingsService.updatePasswordPolicy(channel, request));
    }

    @PutMapping("/{channel}/mfa-policy")
    @Operation(summary = "Update MFA policy for channel")
    public ResponseEntity<ApiResponse<BackofficeMfaPolicyDetailsResponse>> updateMfaPolicy(
            @PathVariable Channel channel,
            @Valid @RequestBody BackofficeMfaPolicyUpdateRequest request
    ) {
        return ResponseBuilder.success(backofficeSecuritySettingsService.updateMfaPolicy(channel, request));
    }

    @PutMapping("/{channel}/security-question-policy")
    @Operation(summary = "Update security question policy for channel")
    public ResponseEntity<ApiResponse<BackofficeSecurityQuestionPolicyDetailsResponse>> updateSecurityQuestionPolicy(
            @PathVariable Channel channel,
            @Valid @RequestBody BackofficeSecurityQuestionPolicyUpdateRequest request
    ) {
        return ResponseBuilder.success(backofficeSecuritySettingsService.updateSecurityQuestionPolicy(channel, request));
    }

    @PutMapping("/{channel}/session-policy")
    @Operation(summary = "Update session policy for channel")
    public ResponseEntity<ApiResponse<SessionPolicyResponse>> updateSessionPolicy(
            @PathVariable Channel channel,
            @Valid @RequestBody BackofficeSessionPolicyUpdateRequest request
    ) {
        return ResponseBuilder.success(backofficeSecuritySettingsService.updateSessionPolicy(channel, request));
    }
}
