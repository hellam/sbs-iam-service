package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.api.request.RefreshTokenRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginHistoryService;
import ke.shiva.sbs_iam.modules.iam.app.service.OidcTokenService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication Flow")
public class FinalizeLoginController {

    private final LoginFlowService loginFlowService;
    private final OidcTokenService oidcTokenService;
    private final LoginHistoryService loginHistoryService;

    @Operation(summary = "9. Finalize Login")
    @PostMapping("/finalize")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<ApiResponse<OidcTokenResponse>> finalize(
            @FlowId UUID flowId
    ) {
        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.MFA_OK);

        LoginRequirements reqs = loginFlowService.getRequirements(session);

        if (reqs.hasPostLoginSteps()) {
            log.error("Attempt to finalize login with pending post-login steps, flowId={}", flowId);
            throw BaseException.forbidden("Access denied");
        }

        // Extract identifier from session metadata
        String identifier = loginFlowService.extractIdentifier(session);

        // No profile selection required
        session.setProfileType(null);
        session.setProfileId(null);
        session.setSessionType(SessionType.LOGIN_ACTIVE);
        loginFlowService.save(session);

        // Log successful login completion
        loginHistoryService.logLoginSuccess(session.getIamUser(), identifier, session);

        return ResponseBuilder.success(oidcTokenService.issueTokens(session.getId()));
    }

    @Operation(summary = "Refresh OIDC tokens")
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<OidcTokenResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request,
            @CookieValue(value = SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME) String deviceId
    ) {
        return ResponseBuilder.success(oidcTokenService.refreshTokens(request, deviceId));
    }
}
