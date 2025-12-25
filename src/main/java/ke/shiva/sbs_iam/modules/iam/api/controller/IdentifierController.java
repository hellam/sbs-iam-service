package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.IdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.IdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.DomainGuard;
import ke.shiva.sbs_iam.modules.iam.app.service.IdentifierService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.Channel;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth/identifier")
@RequiredArgsConstructor
@Tag(name = "Authentication Flow")
public class IdentifierController {

    private final IdentifierService identifierService;
    private final DomainGuard domainGuard;

    @PostMapping("/backoffice")
    @Operation(summary = "1. Identify User (Backoffice)")
    public ResponseEntity<ApiResponse<IdentifierResponse>> identifyBackoffice(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http
    ) {
        domainGuard.validate(Channel.BACKOFFICE, http);
        req.setChannel(Channel.BACKOFFICE);
        return ResponseBuilder.success("User identified successfully", identifierService.handle(req));
    }

    @PostMapping("/mobile")
    @Operation(summary = "1. Identify User (Mobile)")
    public ResponseEntity<IdentifierResponse> identifyMobile(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http
    ) {
        domainGuard.validate(Channel.MOBILE_BANKING, http);
        req.setChannel(Channel.MOBILE_BANKING);
        return ResponseEntity.ok(identifierService.handle(req));
    }

    @PostMapping("/internet-banking")
    @Operation(summary = "1. Identify User (Internet Banking)")
    public ResponseEntity<IdentifierResponse> identifyIB(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http
    ) {
        domainGuard.validate(Channel.INTERNET_BANKING, http);
        req.setChannel(Channel.INTERNET_BANKING);
        return ResponseEntity.ok(identifierService.handle(req));
    }
}

