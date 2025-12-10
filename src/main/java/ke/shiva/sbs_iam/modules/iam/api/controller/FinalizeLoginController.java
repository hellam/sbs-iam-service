package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.FinalizeLoginRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.app.service.OidcTokenService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.SessionType;
import ke.shiva.sbs_iam.modules.iam.domain.model.LoginRequirements;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.exception.BaseException;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class FinalizeLoginController {

    private final LoginFlowService loginFlowService;
    private final OidcTokenService oidcTokenService;

    @PostMapping("/finalize")
    public ResponseEntity<ApiResponse<OidcTokenResponse>> finalize(
            @RequestBody @Valid FinalizeLoginRequest req
    ) throws AuthException {
        SessionEntity session =
                loginFlowService.requireStage(req.getFlowId(), LoginStage.MFA_OK);

        LoginRequirements reqs =
                loginFlowService.getRequirements(session);

        if (reqs.hasPostLoginSteps()) {
            throw BaseException.forbidden("Access denied");
        }

        // No profile selection required
        session.setProfileType(null);
        session.setProfileId(null);
        session.setSessionType(SessionType.LOGIN_ACTIVE);

        loginFlowService.updateStage(session, LoginStage.ACTIVE);

        return ResponseBuilder.success(oidcTokenService.issueTokens(session));
    }
}

