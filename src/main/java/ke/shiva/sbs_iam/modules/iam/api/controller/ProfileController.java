package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.ProfileService;
import ke.shiva.shivacorestarter.dto.ApiResponse;
import ke.shiva.shivacorestarter.util.ResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profiles")
    public ResponseEntity<ApiResponse<ProfileSelectionResponse>> listProfiles(
            @RequestParam UUID flowId
    ) {
        return ResponseBuilder.success(profileService.listProfiles(flowId));
    }

    @PostMapping("/profiles/select")
    public ResponseEntity<ApiResponse<OidcTokenResponse>> select(
            @Valid @RequestBody ProfileSelectRequest req
    ) {
        return ResponseBuilder.success(profileService.selectProfile(req));
    }
}

