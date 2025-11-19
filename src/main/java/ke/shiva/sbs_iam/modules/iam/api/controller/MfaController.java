package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.app.service.MfaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/mfa/init")
    public ResponseEntity<MfaInitResponse> initiate(@Valid @RequestBody MfaInitRequest req) {
        return ResponseEntity.ok(mfaService.initiate(req));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<MfaVerifyResponse> verify(@Valid @RequestBody MfaVerifyRequest req) {
        return ResponseEntity.ok(mfaService.verify(req));
    }
}

