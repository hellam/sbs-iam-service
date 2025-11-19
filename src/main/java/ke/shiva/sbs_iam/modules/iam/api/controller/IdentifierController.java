package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.IdentifierRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.IdentifierResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.DomainGuard;
import ke.shiva.sbs_iam.modules.iam.app.service.IdentifierService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.identity.ChannelEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class IdentifierController {

    private final IdentifierService identifierService;
    private final DomainGuard domainGuard;

    @PostMapping("/identifier/backoffice")
    public ResponseEntity<IdentifierResponse> identifyBackoffice(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http
    ) {
        domainGuard.validate(ChannelEnum.BACKOFFICE, http);
        req.setChannel(ChannelEnum.BACKOFFICE);
        return ResponseEntity.ok(identifierService.handle(req));
    }

    @PostMapping("/identifier/mobile")
    public ResponseEntity<IdentifierResponse> identifyMobile(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http
    ) {
        domainGuard.validate(ChannelEnum.MOBILE_BANKING, http);
        req.setChannel(ChannelEnum.MOBILE_BANKING);
        return ResponseEntity.ok(identifierService.handle(req));
    }

    @PostMapping("/identifier/internet-banking")
    public ResponseEntity<IdentifierResponse> identifyIB(
            @RequestBody @Valid IdentifierRequest req,
            HttpServletRequest http
    ) {
        domainGuard.validate(ChannelEnum.INTERNET_BANKING, http);
        req.setChannel(ChannelEnum.INTERNET_BANKING);
        return ResponseEntity.ok(identifierService.handle(req));
    }
}

