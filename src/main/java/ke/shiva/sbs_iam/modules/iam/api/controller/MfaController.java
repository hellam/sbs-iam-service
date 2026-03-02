package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaPolicyResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.TotpEnrollmentInitResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.TotpEnrollmentVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.MfaService;
import ke.shiva.sbs_iam.modules.iam.app.service.TotpEnrollmentService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.ratelimit.KeyType;
import ke.shiva.shivacorestarter.ratelimit.RateLimit;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/mfa")
@RequiredArgsConstructor
@Tag(name = "MFA Verification")
@RateLimit(capacity = 3, refillTokens = 3, refillDuration = "PT10M", keyType = KeyType.IP,
          message = "Too many MFA attempts. Please try again in 10 minutes.")
public class MfaController {

    private final MfaService mfaService;
    private final TotpEnrollmentService totpEnrollmentService;

    @Operation(summary = "3. Initiate MFA")
    @PostMapping("/initiate")
    @RequiresStage(LoginStage.PASSWORD_OK)
    public ResponseEntity<ApiResponse<Void>> initiate(@Valid @RequestBody MfaInitRequest req, @FlowId UUID flowId) {
        return ResponseBuilder.success(mfaService.initiate(req, flowId));
    }

    @Operation(summary = "4. Verify MFA")
    @PostMapping("/verify")
    @RequiresStage(LoginStage.PASSWORD_OK)
    public ResponseEntity<ApiResponse<MfaVerifyResponse>> verify(@Valid @RequestBody MfaVerifyRequest req, @FlowId UUID flowId) {
        return ResponseBuilder.success("MFA verified successfully", mfaService.verify(req, flowId));
    }

    @Operation(summary = "5. Initiate TOTP Enrollment")
    @PostMapping("/totp/enrollment/initiate")
    @RequiresStage(LoginStage.TOTP_ENROLL_REQUIRED)
    public ResponseEntity<ApiResponse<TotpEnrollmentInitResponse>> initiateTotpEnrollment(@FlowId UUID flowId) {
        return ResponseBuilder.success("TOTP enrollment initiated.", totpEnrollmentService.initiate(flowId));
    }

    @Operation(summary = "6. Verify TOTP Enrollment")
    @PostMapping("/totp/enrollment/verify")
    @RequiresStage(LoginStage.TOTP_ENROLL_REQUIRED)
    public ResponseEntity<ApiResponse<TotpEnrollmentVerifyResponse>> verifyTotpEnrollment(
            @Valid @RequestBody MfaVerifyRequest req,
            @FlowId UUID flowId
    ) {
        return ResponseBuilder.success(
                "TOTP enabled successfully. Please sign in again.",
                totpEnrollmentService.verify(flowId, req.getCode())
        );
    }

    @Operation(summary = "Get MFA Policy by Channel")
    @GetMapping("/policy/{channel}")
    public ResponseEntity<ApiResponse<MfaPolicyResponse>> getMfaPolicy(@PathVariable Channel channel) {
        return ResponseBuilder.success(mfaService.getMfaPolicy(channel));
    }
}
