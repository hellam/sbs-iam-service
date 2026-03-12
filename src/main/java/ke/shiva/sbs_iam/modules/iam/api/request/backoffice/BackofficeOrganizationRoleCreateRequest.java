package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ke.shiva.client.iam.enums.TaskRole;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BackofficeOrganizationRoleCreateRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "taskRole is required")
    private TaskRole taskRole;

    private Boolean isDefault;

    @NotEmpty(message = "featureIds is required")
    private List<@NotNull(message = "featureId is required") @Positive(message = "featureId must be positive") Long> featureIds;
}
