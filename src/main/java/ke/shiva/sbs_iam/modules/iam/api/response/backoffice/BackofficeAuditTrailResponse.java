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
    private String source;
    private String eventType;
    private String eventLabel;
    private String userCategory;
    private String channel;
    private String channelLabel;
    private String ipAddress;
    private String deviceId;
    private String entityType;
    private String entityLabel;
    private Long entityId;
    private String details;
    private OffsetDateTime createdAt;
    private Map<String, Object> metadata;
}
