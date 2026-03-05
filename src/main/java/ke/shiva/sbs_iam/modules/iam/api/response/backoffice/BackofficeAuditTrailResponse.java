package ke.shiva.sbs_iam.modules.iam.api.response.backoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackofficeAuditTrailResponse {
    private Long id;
    private String eventType;
    private String userCategory;
    private String channel;
    private String ipAddress;
    private String deviceId;
    private String entityType;
    private Long entityId;
    private OffsetDateTime createdAt;
    private Map<String, Object> metadata;
}
