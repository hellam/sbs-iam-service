package ke.shiva.sbs_iam.modules.analytics.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ke.shiva.sbs_iam.modules.analytics.infra.repository.IamDashboardSnapshotQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(value = "analytics.events.enabled", havingValue = "true", matchIfMissing = true)
public class IamAnalyticsSnapshotProducer {

    private static final Logger log = LoggerFactory.getLogger(IamAnalyticsSnapshotProducer.class);

    private static final int ONLINE_WINDOW_MINUTES = 15;
    private static final String SNAPSHOT_EVENT_TYPE = "DASHBOARD_SNAPSHOT";
    private static final String SOURCE = "IAM";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IamDashboardSnapshotQueryRepository queryRepository;

    @Value("${analytics.events.topic:analytics.dashboard.snapshot.v1}")
    private String topic;

    public IamAnalyticsSnapshotProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            IamDashboardSnapshotQueryRepository queryRepository
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.queryRepository = queryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishStartupSnapshot() {
        publishSnapshot();
    }

    @Scheduled(cron = "${analytics.events.snapshot-cron:0 */1 * * * *}")
    public void publishScheduledSnapshot() {
        publishSnapshot();
    }

    private void publishSnapshot() {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            LocalDate endDate = now.toLocalDate();
            LocalDate startDate = endDate.minusDays(6);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("totalUsers", queryRepository.countTotalUsers());
            payload.put("onlineUsers", queryRepository.countOnlineUsers(ONLINE_WINDOW_MINUTES));
            payload.put("failedLogins24h", queryRepository.countFailedLoginsSince(now.minusHours(24)));
            payload.put("lockedAccounts", queryRepository.countLockedAccounts());
            payload.put("weeklyLoggedInUsers", weeklyTrend(startDate, endDate));

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("source", SOURCE);
            event.put("eventType", SNAPSHOT_EVENT_TYPE);
            event.put("generatedAt", now);
            event.put("payload", payload);

            kafkaTemplate.send(topic, SOURCE, objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            log.error("Failed publishing IAM analytics snapshot", exception);
        }
    }

    private List<Map<String, Object>> weeklyTrend(LocalDate startDate, LocalDate endDate) {
        return queryRepository.findWeeklyLoggedInUsers(startDate, endDate).stream()
                .map(point -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("day", point.day().toString());
                    row.put("value", point.value());
                    return row;
                })
                .toList();
    }
}
