package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.api.request.*;
import ke.shiva.sbs_iam.modules.iam.api.response.*;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.*;
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
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
@Tag(name = "Forgot Password")
public class ForgotPasswordController {

    private final ForgotPasswordIdentifierService identifierService;
    private final ForgotPasswordSecurityQuestionsService securityQuestionsService;
    private final ForgotPasswordMfaService mfaService;
    private final ForgotPasswordResetService resetService;
    private final DomainGuard domainGuard;

    @Operation(summary = "1. Initiate Forgot Password - Backoffice")
    @PostMapping("/identifier/backoffice")
    @RateLimit(capacity = 5, refillTokens = 5, refillDuration = "PT5M", keyType = KeyType.IP,
            message = "Too many forgot password attempts. Please try again in 5 minutes.")
    public ResponseEntity<ApiResponse<ForgotPasswordIdentifierResponse>> identifierBackoffice(
            @RequestBody @Valid ForgotPasswordIdentifierRequest req,
            HttpServletRequest request
    ) {
        String deviceId = request.getHeader(SecurityConstants.Headers.DEVICE_ID);
        domainGuard.validate(Channel.BACKOFFICE, request);
        req.setChannel(Channel.BACKOFFICE);
        return ResponseBuilder.success(identifierService.handle(req, deviceId));
    }

    @Operation(summary = "1. Initiate Forgot Password - Mobile")
    @PostMapping("/identifier/mobile")
    @RateLimit(capacity = 5, refillTokens = 5, refillDuration = "PT5M", keyType = KeyType.IP,
            message = "Too many forgot password attempts. Please try again in 5 minutes.")
    public ResponseEntity<ApiResponse<ForgotPasswordIdentifierResponse>> identifierMobile(
            @RequestBody @Valid ForgotPasswordIdentifierRequest req,
            HttpServletRequest httpRequest
    ) {
        String deviceId = httpRequest.getHeader(SecurityConstants.Headers.DEVICE_ID);
        domainGuard.validate(Channel.MOBILE_BANKING, httpRequest);
        req.setChannel(Channel.MOBILE_BANKING);
        return ResponseBuilder.success(identifierService.handle(req, deviceId));
    }

    @Operation(summary = "1. Initiate Forgot Password - Internet Banking")
    @PostMapping("/identifier/internet-banking")
    @RateLimit(capacity = 5, refillTokens = 5, refillDuration = "PT5M", keyType = KeyType.IP,
            message = "Too many forgot password attempts. Please try again in 5 minutes.")
    public ResponseEntity<ApiResponse<ForgotPasswordIdentifierResponse>> identifierIB(
            @RequestBody @Valid ForgotPasswordIdentifierRequest req,
            HttpServletRequest httpRequest
    ) {
        String deviceId = httpRequest.getHeader(SecurityConstants.Headers.DEVICE_ID);
        domainGuard.validate(Channel.INTERNET_BANKING, httpRequest);
        req.setChannel(Channel.INTERNET_BANKING);
        return ResponseBuilder.success(identifierService.handle(req, deviceId));
    }

    @Operation(summary = "2. Verify Security Questions")
    @PostMapping("/security-questions/verify")
    @RequiresStage(LoginStage.FP_IDENTIFIER_OK)
    @RateLimit(capacity = 3, refillTokens = 3, refillDuration = "PT10M", keyType = KeyType.IP,
            message = "Too many security question attempts. Please try again in 10 minutes.")
    public ResponseEntity<ApiResponse<ForgotPasswordSecurityQuestionsResponse>> verifySecurityQuestions(
            @RequestBody @Valid ForgotPasswordSecurityQuestionsRequest req,
            @FlowId UUID flowId
    ) {
        return ResponseBuilder.success(securityQuestionsService.handle(req, flowId));
    }

    @Operation(summary = "3. Initiate MFA for Forgot Password")
    @PostMapping("/mfa/initiate")
    @RateLimit(capacity = 3, refillTokens = 3, refillDuration = "PT10M", keyType = KeyType.IP,
            message = "Too many MFA attempts. Please try again in 10 minutes.")
    public ResponseEntity<ApiResponse<MfaInitResponse>> initiateMfa(
            @Valid @RequestBody MfaInitRequest req,
            @FlowId UUID flowId
    ) {
        return ResponseBuilder.success("MFA initiated successfully", mfaService.initiate(req, flowId));
    }

    @Operation(summary = "4. Verify MFA for Forgot Password")
    @PostMapping("/mfa/verify")
    @RateLimit(capacity = 3, refillTokens = 3, refillDuration = "PT10M", keyType = KeyType.IP,
            message = "Too many MFA attempts. Please try again in 10 minutes.")
    public ResponseEntity<ApiResponse<MfaVerifyResponse>> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest req,
            @FlowId UUID flowId
    ) {
        return ResponseBuilder.success("MFA verified successfully", mfaService.verify(req, flowId));
    }

    @Operation(summary = "5. Reset Password")
    @PostMapping("/reset")
    @RateLimit(capacity = 3, refillTokens = 3, refillDuration = "PT10M", keyType = KeyType.IP,
            message = "Too many password reset attempts. Please try again in 10 minutes.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ForgotPasswordResetRequest req,
            @FlowId UUID flowId
    ) {
        resetService.handle(req, flowId);
        return ResponseBuilder.success("Password has been reset successfully. You can now log in with your new password.");
    }
}
