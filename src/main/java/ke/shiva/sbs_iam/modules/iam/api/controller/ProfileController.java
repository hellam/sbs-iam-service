package ke.shiva.sbs_iam.modules.iam.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.UserProfileResponse;
import ke.shiva.sbs_iam.modules.iam.app.security.FlowId;
import ke.shiva.sbs_iam.modules.iam.app.security.RequiresStage;
import ke.shiva.sbs_iam.modules.iam.app.service.ProfileService;
import ke.shiva.sbs_iam.modules.iam.domain.enums.LoginStage;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
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

    @Operation(summary = "List Profiles for Active Session")
    @GetMapping("/session/profiles")
    public ResponseEntity<ApiResponse<ProfileSelectionResponse>> listSessionProfiles(@AuthenticationPrincipal Jwt jwt) {
        return ResponseBuilder.success(profileService.listSessionProfiles(jwt));
    }

    @Operation(summary = "8. Select Profile")
    @PostMapping("/profiles/select")
    @RequiresStage(LoginStage.MFA_OK)
    public ResponseEntity<ApiResponse<UserProfileResponse>> select(
            @Valid @RequestBody ProfileSelectRequest req, @FlowId UUID flowId
    ) {
        return ResponseBuilder.success("Profile selected successfully", profileService.selectProfile(req, flowId));
    }

    @Operation(summary = "Switch Active Profile")
    @PostMapping("/session/profiles/switch")
    public ResponseEntity<ApiResponse<UserProfileResponse>> switchProfile(
            @Valid @RequestBody ProfileSelectRequest req,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseBuilder.success("Profile switched successfully", profileService.switchProfile(req, jwt));
    }
}
