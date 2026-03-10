package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BackofficeOrganizationUserAddRequest {

    @NotNull(message = "iamUserId is required")
    @Positive(message = "iamUserId must be positive")
    private Long iamUserId;

    @NotEmpty(message = "clientAccountIds is required")
    private List<@NotBlank(message = "clientAccountId cannot be blank") String> clientAccountIds;
}
