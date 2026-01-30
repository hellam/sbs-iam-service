package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaInitRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.MfaVerifyRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaInitResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.MfaVerifyResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.MfaService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.ratelimit.KeyType;
import ke.shiva.shivacorestarter.ratelimit.RateLimit;
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
@Tag(name = "MFA Verification")
@RateLimit(capacity = 3, refillTokens = 3, refillDuration = "PT10M", keyType = KeyType.IP,
          message = "Too many MFA attempts. Please try again in 10 minutes.")
public class MfaController {

    private final MfaService mfaService;

    @Operation(summary = "3. Initiate MFA")
    @PostMapping("/initiate")
    @RequiresStage(LoginStage.PASSWORD_OK)
    public ResponseEntity<ApiResponse<MfaInitResponse>> initiate(@Valid @RequestBody MfaInitRequest req, @FlowId UUID flowId) {
        return ResponseBuilder.success(mfaService.initiate(req, flowId));
    }

    @Operation(summary = "4. Verify MFA")
    @PostMapping("/verify")
    @RequiresStage(LoginStage.PASSWORD_OK)
    public ResponseEntity<ApiResponse<MfaVerifyResponse>> verify(@Valid @RequestBody MfaVerifyRequest req, @FlowId UUID flowId) {
        return ResponseBuilder.success("MFA verified successfully", mfaService.verify(req, flowId));
    }
}

