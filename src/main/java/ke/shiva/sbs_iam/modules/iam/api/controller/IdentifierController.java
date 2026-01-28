package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.config.SecurityConfig.SecurityConstants;
import ke.shiva.sbs_iam.modules.iam.api.request.IdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.IdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.DomainGuard;
import ke.shiva.sbs_iam.modules.iam.app.service.IdentifierService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.ratelimit.KeyType;
import ke.shiva.shivacorestarter.ratelimit.RateLimit;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("identifier")
@RequiredArgsConstructor
@Tag(name = "Authentication Flow")
@RateLimit(capacity = 10, refillTokens = 10, refillDuration = "PT1M", keyType = KeyType.IP,
          message = "Too many identifier lookup attempts. Please try again in a minute.")
public class IdentifierController {

    private final IdentifierService identifierService;
    private final DomainGuard domainGuard;

    @Operation(summary = "1. Identify User (Backoffice)")
    @PostMapping("/backoffice")
    public ResponseEntity<ApiResponse<IdentifierResponse>> identifyBackoffice(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest request
    ) {
        String deviceId = request.getHeader(SecurityConstants.Headers.DEVICE_ID);
        domainGuard.validate(Channel.BACKOFFICE, request);
        req.setChannel(Channel.BACKOFFICE);
        return ResponseBuilder.success(identifierService.handle(req, deviceId));
    }

    @Operation(summary = "1. Identify User (Mobile)")
    @PostMapping("/mobile")
    public ResponseEntity<ApiResponse<IdentifierResponse>> identifyMobile(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http,
            //TODO: Change to DEVICE_ID_TOKEN_NAME not Cookie
            @CookieValue(value = SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME) String deviceId
    ) {
        domainGuard.validate(Channel.MOBILE_BANKING, http);
        req.setChannel(Channel.MOBILE_BANKING);
        return ResponseBuilder.success(identifierService.handle(req, deviceId));
    }

    @Operation(summary = "1. Identify User (Internet Banking)")
    @PostMapping("/internet-banking")
    public ResponseEntity<ApiResponse<IdentifierResponse>> identifyIB(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http,
            @CookieValue(value = SecurityConstants.Cookies.DEVICE_ID_TOKEN_NAME) String deviceId
    ) {
        domainGuard.validate(Channel.INTERNET_BANKING, http);
        req.setChannel(Channel.INTERNET_BANKING);
        return ResponseBuilder.success(identifierService.handle(req, deviceId));
    }

//    @Operation(summary = "1. Identify User (USSD)")
//    @PostMapping("/ussd")
//    public ResponseEntity<IdentifierResponse> identifyUssd(
//            @RequestBody @Valid IdentifierRequest req,
//            HttpServletRequest http
//    ) {
//        domainGuard.validate(Channel.USSD, http);
//        req.setChannel(Channel.USSD);
//        return ResponseEntity.ok(identifierService.handle(req));
//    }
}
