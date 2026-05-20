package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BackofficeOrganizationUserAccountsUpdateRequest {

    @NotNull(message = "clientAccountIds is required")
    private List<@NotBlank(message = "clientAccountId cannot be blank") String> clientAccountIds = List.of();
}
