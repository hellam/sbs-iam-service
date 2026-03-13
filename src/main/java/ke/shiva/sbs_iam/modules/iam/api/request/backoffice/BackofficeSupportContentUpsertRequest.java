package ke.shiva.sbs_iam.modules.iam.api.request.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackofficeSupportContentUpsertRequest {

    @NotNull(message = "category is required")
    private String category;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;

    private String subtitle;
    private String contactChannel;
    private String contactValue;
    private Integer sortOrder;
    private Boolean isActive;
}
