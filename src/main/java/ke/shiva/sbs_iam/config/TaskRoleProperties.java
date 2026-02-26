package ke.shiva.sbs_iam.config;

import ke.shiva.sbs_iam.modules.iam.domain.enums.organization.TaskRole;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "iam.task-roles")
public class TaskRoleProperties {

    /**
     * Display labels by canonical task role.
     */
    private Map<TaskRole, String> labels = new EnumMap<>(TaskRole.class);

    /**
     * Long-form descriptions by canonical task role.
     */
    private Map<TaskRole, String> descriptions = new EnumMap<>(TaskRole.class);
}
