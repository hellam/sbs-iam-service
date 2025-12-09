package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.api.request.ProfileSelectRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.OidcTokenResponse;
import ke.shiva.sbs_iam.modules.iam.api.response.ProfileSelectionResponse;
import ke.shiva.sbs_iam.modules.iam.app.service.ProfileService;
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
    public ResponseEntity<ProfileSelectionResponse> listProfiles(
            @RequestParam UUID flowId
    ) throws AuthException {
        return ResponseEntity.ok(profileService.listProfiles(flowId));
    }

    @PostMapping("/profiles/select")
    public ResponseEntity<OidcTokenResponse> select(
            @Valid @RequestBody ProfileSelectRequest req
    ) throws AuthException {
        return ResponseEntity.ok(profileService.selectProfile(req));
    }
}

