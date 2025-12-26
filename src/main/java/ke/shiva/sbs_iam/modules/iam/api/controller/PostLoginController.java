package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.SecurityQuestionsRequest;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.PostLoginService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import lombok.RequiredArgsConstructor;
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
public class PostLoginController {

    private final PostLoginService postLoginService;

    @Operation(summary = "5. Change Password")
    @PostMapping("/password/change")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid PasswordChangeRequest req, @FlowId UUID flowId
    ) {
        postLoginService.changePassword(req,flowId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "6. Submit Security Questions")
    @PostMapping("/security-questions")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<Void> submitQuestions(
            @RequestBody @Valid SecurityQuestionsRequest req, @FlowId UUID flowId
    ) {
        postLoginService.handleQuestions(req, flowId);
        return ResponseEntity.ok().build();
    }
}

