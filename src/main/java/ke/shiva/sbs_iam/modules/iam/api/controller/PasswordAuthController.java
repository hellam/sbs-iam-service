package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordLoginRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.PasswordStepResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class PasswordAuthController {

    private final PasswordAuthService passwordAuthService;

    @PostMapping("/password")
    public ResponseEntity<PasswordStepResponse> passwordStep(
            @RequestBody @Valid PasswordLoginRequest request
    ) {
        return ResponseEntity.ok(passwordAuthService.handle(request));
    }
}
