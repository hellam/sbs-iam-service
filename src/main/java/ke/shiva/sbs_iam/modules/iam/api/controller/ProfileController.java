package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.app.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profiles")
    public ResponseEntity<ProfileSelectionResponse> listProfiles(
            @RequestParam UUID flowId
    ) {
        return ResponseEntity.ok(profileService.listProfiles(flowId));
    }

    @PostMapping("/profiles/select")
    public ResponseEntity<OidcTokenResponse> select(
            @Valid @RequestBody ProfileSelectRequest req
    ) {
        return ResponseEntity.ok(profileService.selectProfile(req));
    }
}

