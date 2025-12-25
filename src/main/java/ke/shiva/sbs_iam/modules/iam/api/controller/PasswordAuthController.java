package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordLoginRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.PasswordStepResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.PasswordAuthService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Tag(name = "Authentication Flow")
public class PasswordAuthController {

    private final PasswordAuthService passwordAuthService;

    @PostMapping("/password")
    @RequiresStage(LoginStage.IDENTIFIER_OK)
    @Operation(summary = "2. Submit Password")
    public ResponseEntity<ApiResponse<PasswordStepResponse>> passwordStep(
            @RequestBody @Valid PasswordLoginRequest request, @FlowId UUID flowId
    ) {
        return ResponseBuilder.success(passwordAuthService.handle(request, flowId));
    }
}
