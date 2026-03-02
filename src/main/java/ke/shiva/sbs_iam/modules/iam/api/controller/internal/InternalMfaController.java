package ke.shiva.sbs_iam.modules.iam.api.controller.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.service.CommonMfaService;
import ke.shiva.sbs_iam.modules.iam.app.service.LoginFlowService;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/mfa")
@RequiredArgsConstructor
@Tag(name = "Internal MFA Operations", description = "Endpoints for internal MFA operations Not exposed to external clients.")
public class InternalMfaController {
    private final CommonMfaService commonMfaService;
    private final LoginFlowService loginFlowService;

    @Operation(summary = "1. Initiate MFA - Supports OTP Only")
    @PostMapping("/initiate")
    public  ResponseEntity<ApiResponse<Void>> initiateMfa(@Valid @RequestBody MfaInitRequest req, @FlowId UUID flowId) {
        SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.ACTIVE);
        String message = commonMfaService.sendOtp(session, req.getChannel());
        return ResponseBuilder.success(message);
    }

     @Operation(summary = "2. Verify MFA - Supports OTP and TOTP")
     @PostMapping("/verify")
     public ResponseEntity<ApiResponse<Void>> verifyMfa(@Valid @RequestBody MfaVerifyRequest req, @FlowId UUID flowId) {
         SessionEntity session = loginFlowService.requireStage(flowId, LoginStage.ACTIVE);

         commonMfaService.verify(session, req.getCode(), req.getAction());
         return ResponseBuilder.success("MFA verified successfully");
     }
}
