package ke.shiva.sbs_iam.modules.iam.api.controller;

import jakarta.validation.Valid;
import ke.shiva.sbs_iam.modules.iam.app.service.PostLoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class PostLoginController {

    private final PostLoginService postLoginService;

    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(
            @RequestBody @Valid PasswordChangeRequest req
    ) {
        postLoginService.changePassword(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/security-questions")
    public ResponseEntity<Void> submitQuestions(
            @RequestBody @Valid SecurityQuestionsRequest req
    ) {
        postLoginService.handleQuestions(req);
        return ResponseEntity.ok().build();
    }
}

