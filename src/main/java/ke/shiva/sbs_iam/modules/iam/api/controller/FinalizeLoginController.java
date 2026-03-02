package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.api.request.RefreshTokenRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.UserProfileResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.IamUserService;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginHistoryService;
import ke.shiva.sbs_iam.modules.iam.app.service.OidcTokenService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
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
    private final IamUserService iamUserService;

    @Operation(summary = "9. Finalize Login")
    @PostMapping("/finalize")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<ApiResponse<UserProfileResponse>> finalize(
            @FlowId UUID flowId
    ) {
        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.MFA_OK);

        //Not allowed for internet banking
        if (session.getChannel() == Channel.INTERNET_BANKING)
            throw BaseException.forbidden("Access denied");

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

        oidcTokenService.issueTokens(session.getId());



        return ResponseBuilder.success("Welcome! You have successfully logged in.", UserProfileResponse.builder()
                .identifier(identifier)
                .profileType(session.getProfileType())
                .displayName(iamUserService.getIamUserFullName(session.getIamUser().getId()))
                .isOrganisation(Boolean.FALSE)
                .build());
    }

    @Operation(summary = "Refresh OIDC tokens")
    @PostMapping("/refresh")
    @Hidden
    public ResponseEntity<ApiResponse<Void>> refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {

        String deviceId = httpRequest.getHeader(SecurityConstants.Headers.DEVICE_ID);
        oidcTokenService.refreshTokens(request, deviceId);
        return ResponseBuilder.success("Successfully refreshed tokens.");
    }
}
