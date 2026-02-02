package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.PasswordChangeRequest;
import ke.shiva.sbs_iam.modules.iam.api.request.SecurityQuestionsRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.SecurityQuestionsResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.PostLoginService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication Flow")
public class PostLoginController {

    private final PostLoginService postLoginService;

    @Operation(summary = "Get Security Questions (Public)", description = "Fetch all available security questions. No authentication required.")
    @GetMapping("/security-questions")
    public ResponseEntity<ApiResponse<SecurityQuestionsResponse>> getSecurityQuestions(
            @FlowId UUID flowId
    ) {
        SecurityQuestionsResponse response = postLoginService.getAllSecurityQuestions(flowId);
        return ResponseBuilder.success(response);
    }

    @Operation(summary = "5. Change Password")
    @PostMapping("/password/change")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid PasswordChangeRequest req, @FlowId UUID flowId
    ) {
        postLoginService.changePassword(req,flowId);
        return ResponseBuilder.success("Password changed successfully. Please login again with your new password.");
    }

    @Operation(summary = "6. Submit Security Questions")
    @PostMapping("/security-questions")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<ApiResponse<Void>> submitQuestions(
            @RequestBody @Valid SecurityQuestionsRequest req, @FlowId UUID flowId
    ) {
        postLoginService.handleQuestions(req, flowId);
        return ResponseBuilder.success("Security questions set successfully");
    }
}

