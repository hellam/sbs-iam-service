package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.ProfileService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Tag(name = "Authentication Flow")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "7. List Profiles")
    @GetMapping("/profiles")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<ApiResponse<ProfileSelectionResponse>> listProfiles(@FlowId UUID flowId) {
        return ResponseBuilder.success(profileService.listProfiles(flowId));
    }

    @Operation(summary = "8. Select Profile")
    @PostMapping("/profiles/select")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<ApiResponse<OidcTokenResponse>> select(
            @Valid @RequestBody ProfileSelectRequest req, @FlowId UUID flowId
    ) {
        return ResponseBuilder.success(profileService.selectProfile(req, flowId));
    }
}

