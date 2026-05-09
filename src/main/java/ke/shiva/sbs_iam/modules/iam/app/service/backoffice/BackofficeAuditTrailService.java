package ke.shiva.sbs_iam.modules.iam.app.service.backoffice;

import jakarta.servlet.http.HttpServletRequest;
import ke.shiva.sbs_iam.modules.iam.api.response.backoffice.BackofficeAuditTrailResponse;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.IamAuditLogEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.LoginHistoryEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.audit.SessionEventEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.IamUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.identity.SessionEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.profile.OrganizationUserEntity;
import ke.shiva.sbs_iam.modules.iam.domain.entity.security.SecurityEventEntity;
import ke.shiva.sbs_iam.modules.iam.infra.repository.IamAuditLogRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.LoginHistoryRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SecurityEventRepository;
import ke.shiva.sbs_iam.modules.iam.infra.repository.SessionEventRepository;
import ke.shiva.sbs_iam.modules.iam.app.util.RequestContextExtractor;
import ke.shiva.shivacorestarter.dto.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackofficeAuditTrailService {

    private static final int AUDIT_TRAIL_LIMIT = 100;
    private static final int PROFILE_FETCH_LIMIT = 300;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String CHANNEL_BACKOFFICE = "BACKOFFICE";
    private static final String ENTITY_IAM_AUDIT = "IAM_AUDIT_LOG";
    private static final String ENTITY_LOGIN_HISTORY = "LOGIN_HISTORY";
    private static final String ENTITY_SESSION_EVENT = "SESSION_EVENT";
    private static final String ENTITY_SECURITY_EVENT = "SECURITY_EVENT";
    private static final String ENTITY_ORGANIZATION = "ORGANIZATION";
    private static final String ENTITY_TRANSACTION = "TRANSACTION";
    private static final String SOURCE_IAM = "IAM";
    private static final String SOURCE_LOGIN = "LOGIN";
    private static final String SOURCE_SESSION = "SESSION";
    private static final String SOURCE_SECURITY = "SECURITY";
    private static final String SOURCE_PAYMENTS = "PAYMENTS";

    private final IamAuditLogRepository iamAuditLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SessionEventRepository sessionEventRepository;
    private final SecurityEventRepository securityEventRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RequestContextExtractor requestContextExtractor;

    @Transactional(readOnly = true)
    public List<BackofficeAuditTrailResponse> getUserAuditTrail(IamUserEntity iamUser) {
        Long iamUserId = iamUser != null ? iamUser.getId() : null;
        if (iamUserId == null) {
            return List.of();
        }
        return aggregateForUserIds(List.of(iamUserId), List.of());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BackofficeAuditTrailResponse> getUserAuditTrail(
            IamUserEntity iamUser,
            String customerId,
            HttpServletRequest request
    ) {
        Long iamUserId = iamUser != null ? iamUser.getId() : null;
        if (iamUserId == null) {
            return toPaginatedResponse(List.of(), request);
        }
        return toPaginatedResponse(buildProfileTimeline(List.of(iamUserId), List.of(), customerId), request);
    }

    @Transactional(readOnly = true)
    public List<BackofficeAuditTrailResponse> getOrganizationAuditTrail(
            OrganizationEntity organization,
            Collection<OrganizationUserEntity> organizationUsers
    ) {
        List<Long> iamUserIds = organizationUsers == null
                ? List.of()
                : organizationUsers.stream()
                .map(OrganizationUserEntity::getIamUser)
                .filter(Objects::nonNull)
                .map(IamUserEntity::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Long organizationId = organization != null ? organization.getId() : null;
        List<BackofficeAuditTrailResponse> entityEvents = organizationId == null
                ? List.of()
                : iamAuditLogRepository
                .findTop100ByEntityTypeAndEntityIdOrderByCreatedAtDesc(ENTITY_ORGANIZATION, organizationId)
                .stream()
                .map(this::fromIamAuditLog)
                .toList();

        return aggregateForUserIds(iamUserIds, entityEvents);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BackofficeAuditTrailResponse> getOrganizationAuditTrail(
            OrganizationEntity organization,
            Collection<OrganizationUserEntity> organizationUsers,
            String customerId,
            HttpServletRequest request
    ) {
        List<Long> iamUserIds = organizationUsers == null
                ? List.of()
                : organizationUsers.stream()
                .map(OrganizationUserEntity::getIamUser)
                .filter(Objects::nonNull)
                .map(IamUserEntity::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Long organizationId = organization != null ? organization.getId() : null;
        List<BackofficeAuditTrailResponse> entityEvents = organizationId == null
                ? List.of()
                : iamAuditLogRepository
                .findTop100ByEntityTypeAndEntityIdOrderByCreatedAtDesc(ENTITY_ORGANIZATION, organizationId)
                .stream()
                .map(this::fromIamAuditLog)
                .toList();

        return toPaginatedResponse(buildProfileTimeline(iamUserIds, entityEvents, customerId), request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserAudit(
            IamUserEntity iamUser,
            String userCategory,
            String eventType,
            String channel,
            String entityType,
            Long entityId,
            Map<String, Object> metadata
    ) {
        Long iamUserId = iamUser != null ? iamUser.getId() : null;
        if (iamUserId == null || !StringUtils.hasText(eventType)) {
            return;
        }

        try {
            IamAuditLogEntity entry = new IamAuditLogEntity();
            entry.setIamUser(iamUser);
            entry.setUserCategory(trimToNull(userCategory));
            entry.setEventType(eventType.trim());
            entry.setChannel(trimToNull(channel));
            entry.setEntityType(trimToNull(entityType));
            entry.setEntityId(entityId);
            entry.setCreatedAt(OffsetDateTime.now());
            applyCurrentRequestContext(entry);
            entry.setMetadata(withActor(metadata));
            iamAuditLogRepository.save(entry);
        } catch (Exception exception) {
            log.warn("Unable to write IAM audit log event {} for user {}: {}",
                    eventType, iamUserId, exception.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEntityAudit(String eventType, String entityType, Long entityId, Map<String, Object> metadata) {
        if (!StringUtils.hasText(eventType) || !StringUtils.hasText(entityType) || entityId == null) {
            return;
        }

        try {
            IamAuditLogEntity entry = new IamAuditLogEntity();
            entry.setEventType(eventType.trim());
            entry.setUserCategory("CORPORATE");
            entry.setChannel(CHANNEL_BACKOFFICE);
            entry.setEntityType(entityType.trim());
            entry.setEntityId(entityId);
            entry.setCreatedAt(OffsetDateTime.now());
            applyCurrentRequestContext(entry);
            entry.setMetadata(withActor(metadata));
            iamAuditLogRepository.save(entry);
        } catch (Exception exception) {
            log.warn("Unable to write IAM audit log event {} for entity {}:{}: {}",
                    eventType, entityType, entityId, exception.getMessage());
        }
    }

    private List<BackofficeAuditTrailResponse> aggregateForUserIds(
            Collection<Long> iamUserIds,
            Collection<BackofficeAuditTrailResponse> extraEvents
    ) {
        List<BackofficeAuditTrailResponse> events = new ArrayList<>();
        if (extraEvents != null) {
            events.addAll(extraEvents);
        }

        List<Long> ids = iamUserIds == null
                ? List.of()
                : iamUserIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!ids.isEmpty()) {
            events.addAll(iamAuditLogRepository.findTop100ByIamUser_IdInOrderByCreatedAtDesc(ids).stream()
                    .map(this::fromIamAuditLog)
                    .toList());
            events.addAll(loginHistoryRepository.findTop100ByIamUser_IdInOrderByCreatedAtDesc(ids).stream()
                    .map(this::fromLoginHistory)
                    .toList());
            events.addAll(sessionEventRepository.findTop100BySession_IamUser_IdInOrderByEventAtDesc(ids).stream()
                    .map(this::fromSessionEvent)
                    .toList());
            events.addAll(securityEventRepository.findTop100ByIamUser_IdInOrderByCreatedAtDesc(ids).stream()
                    .map(this::fromSecurityEvent)
                    .toList());
        }

        return events.stream()
                .map(this::decorateForProfile)
                .filter(this::isProfileRelevant)
                .sorted(Comparator.comparing(
                        BackofficeAuditTrailResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(AUDIT_TRAIL_LIMIT)
                .toList();
    }

    private List<BackofficeAuditTrailResponse> buildProfileTimeline(
            Collection<Long> iamUserIds,
            Collection<BackofficeAuditTrailResponse> extraEvents,
            String customerId
    ) {
        List<BackofficeAuditTrailResponse> events = new ArrayList<>();
        if (extraEvents != null) {
            events.addAll(extraEvents);
        }

        List<Long> ids = iamUserIds == null
                ? List.of()
                : iamUserIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!ids.isEmpty()) {
            events.addAll(iamAuditLogRepository.findTop100ByIamUser_IdInOrderByCreatedAtDesc(ids).stream()
                    .map(this::fromIamAuditLog)
                    .toList());
            events.addAll(loginHistoryRepository.findTop100ByIamUser_IdInOrderByCreatedAtDesc(ids).stream()
                    .map(this::fromLoginHistory)
                    .toList());
            events.addAll(sessionEventRepository.findTop100BySession_IamUser_IdInOrderByEventAtDesc(ids).stream()
                    .map(this::fromSessionEvent)
                    .toList());
            events.addAll(securityEventRepository.findTop100ByIamUser_IdInOrderByCreatedAtDesc(ids).stream()
                    .map(this::fromSecurityEvent)
                    .toList());
        }

        events.addAll(fetchPaymentTimeline(customerId));
        enrichTransactionNetworkContext(events);

        return collapseProfileEvents(events.stream()
                .map(this::decorateForProfile)
                .filter(this::isProfileRelevant)
                .sorted(Comparator.comparing(
                        BackofficeAuditTrailResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(PROFILE_FETCH_LIMIT)
                .toList());
    }

    private PaginatedResponse<BackofficeAuditTrailResponse> toPaginatedResponse(
            List<BackofficeAuditTrailResponse> events,
            HttpServletRequest request
    ) {
        int page = parsePositiveInt(request != null ? request.getParameter("page") : null, 1);
        int size = Math.min(
                parsePositiveInt(
                        request != null && request.getParameter("per_page") != null
                                ? request.getParameter("per_page")
                                : request != null ? request.getParameter("perPage") : null,
                        DEFAULT_PAGE_SIZE
                ),
                MAX_PAGE_SIZE
        );

        int total = events == null ? 0 : events.size();
        int zeroBasedPage = Math.max(page - 1, 0);
        int fromIndex = Math.min(zeroBasedPage * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<BackofficeAuditTrailResponse> content = total == 0 ? List.of() : events.subList(fromIndex, toIndex);

        PageImpl<BackofficeAuditTrailResponse> pageData = new PageImpl<>(
                content,
                PageRequest.of(zeroBasedPage, size),
                total
        );
        return new PaginatedResponse<>(
                pageData.getContent(),
                pageData.getNumber(),
                pageData.getTotalPages(),
                pageData.getTotalElements(),
                pageData.getSize(),
                pageData.isFirst(),
                pageData.isLast(),
                pageData.isEmpty()
        );
    }

    private int parsePositiveInt(String rawValue, int fallback) {
        if (!StringUtils.hasText(rawValue)) {
            return fallback;
        }
        try {
            return Math.max(Integer.parseInt(rawValue.trim()), 1);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BackofficeAuditTrailResponse fromIamAuditLog(IamAuditLogEntity entry) {
        return BackofficeAuditTrailResponse.builder()
                .id(entry.getId())
                .source(SOURCE_IAM)
                .eventType(entry.getEventType())
                .userCategory(entry.getUserCategory())
                .channel(entry.getChannel())
                .ipAddress(entry.getIpAddress())
                .deviceId(entry.getDeviceId())
                .entityType(defaultString(entry.getEntityType(), ENTITY_IAM_AUDIT))
                .entityId(entry.getEntityId())
                .createdAt(entry.getCreatedAt())
                .metadata(entry.getMetadata())
                .build();
    }

    private BackofficeAuditTrailResponse fromLoginHistory(LoginHistoryEntity entry) {
        Map<String, Object> metadata = newMetadata();
        putIfPresent(metadata, "identifier_used", entry.getIdentifierUsed());
        putIfPresent(metadata, "failure_reason", entry.getFailureReason());
        putIfPresent(metadata, "user_agent", entry.getUserAgent());
        putIfPresent(metadata, "location_country", entry.getLocationCountry());
        putIfPresent(metadata, "location_city", entry.getLocationCity());

        return BackofficeAuditTrailResponse.builder()
                .id(entry.getId())
                .source(SOURCE_LOGIN)
                .eventType(Boolean.TRUE.equals(entry.getSuccess()) ? "LOGIN_SUCCESS" : "LOGIN_FAILED")
                .userCategory(resolveUserCategory(entry.getIamUser()))
                .channel(entry.getChannel())
                .ipAddress(entry.getIpAddress())
                .deviceId(entry.getDeviceId())
                .entityType(ENTITY_LOGIN_HISTORY)
                .entityId(entry.getId())
                .createdAt(entry.getCreatedAt())
                .metadata(metadata.isEmpty() ? null : metadata)
                .build();
    }

    private BackofficeAuditTrailResponse fromSessionEvent(SessionEventEntity entry) {
        SessionEntity session = entry.getSession();
        Map<String, Object> metadata = copyMetadata(entry.getMetadata());
        if (session != null) {
            putIfPresent(metadata, "session_id", session.getSessionId());
            putIfPresent(metadata, "session_type", session.getSessionType());
            putIfPresent(metadata, "session_status", session.getStatus());
            putIfPresent(metadata, "profile_type", session.getProfileType());
            putIfPresent(metadata, "profile_id", session.getProfileId());
        }

        return BackofficeAuditTrailResponse.builder()
                .id(entry.getId())
                .source(SOURCE_SESSION)
                .eventType(entry.getEventType())
                .userCategory(resolveUserCategory(session != null ? session.getIamUser() : null))
                .channel(session != null && session.getChannel() != null ? session.getChannel().name() : null)
                .ipAddress(defaultString(entry.getIpAddress(), session != null ? session.getIpAddress() : null))
                .deviceId(defaultString(entry.getDeviceId(), session != null ? session.getDeviceId() : null))
                .entityType(ENTITY_SESSION_EVENT)
                .entityId(entry.getId())
                .createdAt(entry.getEventAt())
                .metadata(metadata.isEmpty() ? null : metadata)
                .build();
    }

    private BackofficeAuditTrailResponse fromSecurityEvent(SecurityEventEntity entry) {
        SessionEntity session = entry.getRelatedSession();
        Map<String, Object> metadata = copyMetadata(entry.getMetadata());
        putIfPresent(metadata, "severity", entry.getSeverity());
        putIfPresent(metadata, "description", entry.getDescription());
        putIfPresent(metadata, "location_country", entry.getLocationCountry());
        putIfPresent(metadata, "location_city", entry.getLocationCity());
        if (session != null) {
            putIfPresent(metadata, "session_id", session.getSessionId());
        }

        return BackofficeAuditTrailResponse.builder()
                .id(entry.getId())
                .source(SOURCE_SECURITY)
                .eventType(entry.getEventType())
                .userCategory(resolveUserCategory(entry.getIamUser()))
                .channel(session != null && session.getChannel() != null ? session.getChannel().name() : null)
                .ipAddress(defaultString(entry.getIpAddress(), session != null ? session.getIpAddress() : null))
                .deviceId(defaultString(entry.getDeviceId(), session != null ? session.getDeviceId() : null))
                .entityType(ENTITY_SECURITY_EVENT)
                .entityId(entry.getId())
                .createdAt(entry.getCreatedAt())
                .metadata(metadata.isEmpty() ? null : metadata)
                .build();
    }

    private List<BackofficeAuditTrailResponse> fetchPaymentTimeline(String customerId) {
        String normalizedCustomerId = trimToNull(customerId);
        if (!StringUtils.hasText(normalizedCustomerId)) {
            return List.of();
        }

        String sql = """
                SELECT *
                FROM (
                    SELECT
                        id,
                        transaction_number,
                        transaction_type AS transaction_type,
                        status AS status,
                        maker_transaction_status,
                        customer_id,
                        from_account,
                        to_account,
                        to_account_name,
                        amount,
                        COALESCE(NULLIF(from_currency, ''), NULLIF(to_currency, ''), 'USD') AS currency,
                        channel,
                        created_at::timestamptz AS created_at
                    FROM payments_service.transactions
                    WHERE customer_id = :customerId

                    UNION ALL

                    SELECT
                        id,
                        transaction_number,
                        trx_type AS transaction_type,
                        trx_status AS status,
                        NULL AS maker_transaction_status,
                        customer_id,
                        from_account,
                        beneficiary_account AS to_account,
                        beneficiary_name AS to_account_name,
                        amount,
                        currency,
                        'INTERNET_BANKING' AS channel,
                        created_at
                    FROM payments_service.rtgs_swift_transactions
                    WHERE customer_id = :customerId

                    UNION ALL

                    SELECT
                        id,
                        trx_id AS transaction_number,
                        type AS transaction_type,
                        trx_status AS status,
                        maker_transaction_status,
                        customer_id,
                        from_account,
                        NULL AS to_account,
                        NULL AS to_account_name,
                        amount,
                        currency,
                        'INTERNET_BANKING' AS channel,
                        created_at
                    FROM payments_service.bulk_payments
                    WHERE customer_id = :customerId
                ) profile_transactions
                ORDER BY created_at DESC
                LIMIT :limit
                """;

        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("customerId", normalizedCustomerId)
                    .addValue("limit", PROFILE_FETCH_LIMIT);
            return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
                Long id = rs.getLong("id");
                String transactionType = trimToNull(rs.getString("transaction_type"));
                String status = trimToNull(rs.getString("status"));
                String transactionNumber = trimToNull(rs.getString("transaction_number"));
                String currency = defaultString(rs.getString("currency"), "USD");
                Object amount = rs.getObject("amount");

                Map<String, Object> metadata = newMetadata();
                putIfPresent(metadata, "transaction_number", transactionNumber);
                putIfPresent(metadata, "transaction_type", transactionType);
                putIfPresent(metadata, "status", status);
                putIfPresent(metadata, "maker_transaction_status", rs.getString("maker_transaction_status"));
                putIfPresent(metadata, "customer_id", rs.getString("customer_id"));
                putIfPresent(metadata, "from_account", rs.getString("from_account"));
                putIfPresent(metadata, "to_account", rs.getString("to_account"));
                putIfPresent(metadata, "to_account_name", rs.getString("to_account_name"));
                putIfPresent(metadata, "amount", amount);
                putIfPresent(metadata, "currency", currency);

                return BackofficeAuditTrailResponse.builder()
                        .id(id)
                        .source(SOURCE_PAYMENTS)
                        .eventType("TRANSACTION_" + defaultString(status, "RECORDED").toUpperCase())
                        .eventLabel(paymentEventLabel(transactionType, status))
                        .userCategory("CUSTOMER")
                        .channel(rs.getString("channel"))
                        .channelLabel(toChannelLabel(rs.getString("channel")))
                        .entityType(ENTITY_TRANSACTION)
                        .entityLabel(transactionTypeLabel(transactionType))
                        .entityId(id)
                        .details(paymentDetails(transactionNumber, amount, currency, status))
                        .createdAt(toOffsetDateTime(rs.getObject("created_at")))
                        .metadata(metadata.isEmpty() ? null : metadata)
                        .build();
            });
        } catch (DataAccessException exception) {
            log.warn("Unable to fetch payment audit timeline for customer {}: {}",
                    normalizedCustomerId, exception.getMessage());
            return List.of();
        }
    }

    private List<BackofficeAuditTrailResponse> collapseProfileEvents(List<BackofficeAuditTrailResponse> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        List<BackofficeAuditTrailResponse> result = new ArrayList<>();
        Map<String, BackofficeAuditTrailResponse> sessionRevokedGroups = new LinkedHashMap<>();
        for (BackofficeAuditTrailResponse event : events) {
            if (isSessionRevoked(event)) {
                String key = sessionRevokedGroupKey(event);
                BackofficeAuditTrailResponse existing = sessionRevokedGroups.get(key);
                if (existing == null) {
                    setEventCount(event, 1);
                    sessionRevokedGroups.put(key, event);
                    result.add(event);
                } else {
                    int currentCount = eventCount(existing);
                    setEventCount(existing, currentCount + 1);
                }
            } else {
                result.add(event);
            }
        }

        return result.stream()
                .sorted(Comparator.comparing(
                        BackofficeAuditTrailResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private void enrichTransactionNetworkContext(List<BackofficeAuditTrailResponse> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<NetworkContext> contexts = events.stream()
                .filter(event -> event != null && !ENTITY_TRANSACTION.equalsIgnoreCase(event.getEntityType()))
                .filter(event -> event.getCreatedAt() != null)
                .map(event -> new NetworkContext(
                        event.getCreatedAt(),
                        trimToNull(event.getIpAddress()),
                        trimToNull(event.getDeviceId())
                ))
                .filter(context -> StringUtils.hasText(context.ipAddress()) || StringUtils.hasText(context.deviceId()))
                .sorted(Comparator.comparing(NetworkContext::createdAt, Comparator.reverseOrder()))
                .toList();

        if (contexts.isEmpty()) {
            return;
        }

        for (BackofficeAuditTrailResponse event : events) {
            if (event == null || !ENTITY_TRANSACTION.equalsIgnoreCase(event.getEntityType())) {
                continue;
            }

            boolean missingIp = !StringUtils.hasText(event.getIpAddress());
            boolean missingDevice = !StringUtils.hasText(event.getDeviceId());
            if (!missingIp && !missingDevice) {
                continue;
            }

            NetworkContext publicContext = findNetworkContext(contexts, event.getCreatedAt(), true);
            NetworkContext anyContext = findNetworkContext(contexts, event.getCreatedAt(), false);
            String ipAddress = firstNonBlank(
                    publicContext != null ? publicContext.ipAddress() : null,
                    anyContext != null ? anyContext.ipAddress() : null
            );
            String deviceId = firstNonBlank(
                    publicContext != null ? publicContext.deviceId() : null,
                    anyContext != null ? anyContext.deviceId() : null
            );

            boolean enriched = false;
            if (missingIp && StringUtils.hasText(ipAddress)) {
                event.setIpAddress(ipAddress);
                enriched = true;
            }
            if (missingDevice && StringUtils.hasText(deviceId)) {
                event.setDeviceId(deviceId);
                enriched = true;
            }
            if (enriched) {
                Map<String, Object> metadata = copyMetadata(event.getMetadata());
                metadata.put("network_context_inferred", true);
                event.setMetadata(metadata);
            }
        }
    }

    private NetworkContext findNetworkContext(
            List<NetworkContext> contexts,
            OffsetDateTime eventTime,
            boolean requirePublicIp
    ) {
        if (contexts == null || contexts.isEmpty()) {
            return null;
        }

        for (NetworkContext context : contexts) {
            if (requirePublicIp && !isPublicIpAddress(context.ipAddress())) {
                continue;
            }
            if (eventTime == null || !context.createdAt().isAfter(eventTime)) {
                return context;
            }
        }

        return contexts.stream()
                .filter(context -> !requirePublicIp || isPublicIpAddress(context.ipAddress()))
                .findFirst()
                .orElse(null);
    }

    private boolean isPublicIpAddress(String ipAddress) {
        String normalized = trimToNull(ipAddress);
        if (!StringUtils.hasText(normalized) || "unknown".equalsIgnoreCase(normalized)) {
            return false;
        }
        if (normalized.contains(":")) {
            String lower = normalized.toLowerCase();
            return !lower.equals("::1")
                    && !lower.startsWith("fc")
                    && !lower.startsWith("fd")
                    && !lower.startsWith("fe80");
        }
        String[] parts = normalized.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            if (first == 10 || first == 127 || first == 0) {
                return false;
            }
            if (first == 192 && second == 168) {
                return false;
            }
            if (first == 172 && second >= 16 && second <= 31) {
                return false;
            }
            if (first == 169 && second == 254) {
                return false;
            }
            return first > 0 && first < 224;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private BackofficeAuditTrailResponse decorateForProfile(BackofficeAuditTrailResponse event) {
        if (event == null) {
            return null;
        }

        if (!StringUtils.hasText(event.getSource())) {
            event.setSource(sourceForEntity(event.getEntityType()));
        }
        if (!StringUtils.hasText(event.getEventLabel())) {
            event.setEventLabel(toEventLabel(event.getEventType(), event.getMetadata()));
        }
        if (!StringUtils.hasText(event.getChannelLabel())) {
            event.setChannelLabel(toChannelLabel(event.getChannel()));
        }
        if (!StringUtils.hasText(event.getEntityLabel())) {
            event.setEntityLabel(toEntityLabel(event.getEntityType()));
        }
        event.setDeviceId(maskDeviceId(event.getDeviceId()));
        if (!StringUtils.hasText(event.getDetails())) {
            event.setDetails(toEventDetails(event));
        }
        return event;
    }

    private boolean isProfileRelevant(BackofficeAuditTrailResponse event) {
        if (event == null || !StringUtils.hasText(event.getEventType())) {
            return false;
        }
        if (ENTITY_TRANSACTION.equalsIgnoreCase(event.getEntityType())) {
            return true;
        }
        if (ENTITY_LOGIN_HISTORY.equalsIgnoreCase(event.getEntityType())) {
            return true;
        }
        if (ENTITY_SECURITY_EVENT.equalsIgnoreCase(event.getEntityType())) {
            return true;
        }
        if (ENTITY_SESSION_EVENT.equalsIgnoreCase(event.getEntityType())) {
            return isSessionRevoked(event);
        }

        String code = event.getEventType().toUpperCase();
        return !code.startsWith("SESSION_EXTENDED")
                && !code.startsWith("STAGE_CHANGED")
                && !code.startsWith("PROFILE_SELECTED")
                && !code.equals("LOGIN_FLOW_STARTED");
    }

    private boolean isSessionRevoked(BackofficeAuditTrailResponse event) {
        return event != null && "SESSION_REVOKED".equalsIgnoreCase(event.getEventType());
    }

    private String sessionRevokedGroupKey(BackofficeAuditTrailResponse event) {
        OffsetDateTime createdAt = event.getCreatedAt() == null
                ? OffsetDateTime.MIN
                : event.getCreatedAt().truncatedTo(ChronoUnit.MINUTES);
        String reason = metadataText(event.getMetadata(), "reason");
        return String.join("|",
                defaultString(event.getEventType(), ""),
                defaultString(reason, ""),
                defaultString(event.getUserCategory(), ""),
                createdAt.toString()
        );
    }

    private void setEventCount(BackofficeAuditTrailResponse event, int count) {
        Map<String, Object> metadata = copyMetadata(event.getMetadata());
        metadata.put("event_count", count);
        event.setMetadata(metadata);
        event.setEventLabel(count > 1 ? "Sessions revoked (" + count + ")" : "Session revoked");
        event.setDetails(sessionRevokedDetails(metadata));
    }

    private int eventCount(BackofficeAuditTrailResponse event) {
        Object count = event.getMetadata() != null ? event.getMetadata().get("event_count") : null;
        if (count instanceof Number number) {
            return number.intValue();
        }
        if (count instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private String sessionRevokedDetails(Map<String, Object> metadata) {
        String reason = reasonLabel(metadataText(metadata, "reason"));
        int count = 1;
        Object countValue = metadata != null ? metadata.get("event_count") : null;
        if (countValue instanceof Number number) {
            count = number.intValue();
        }
        return count > 1 ? reason + " · " + count + " sessions" : reason;
    }

    private String sourceForEntity(String entityType) {
        if (ENTITY_LOGIN_HISTORY.equalsIgnoreCase(entityType)) {
            return SOURCE_LOGIN;
        }
        if (ENTITY_SESSION_EVENT.equalsIgnoreCase(entityType)) {
            return SOURCE_SESSION;
        }
        if (ENTITY_SECURITY_EVENT.equalsIgnoreCase(entityType)) {
            return SOURCE_SECURITY;
        }
        if (ENTITY_TRANSACTION.equalsIgnoreCase(entityType)) {
            return SOURCE_PAYMENTS;
        }
        return SOURCE_IAM;
    }

    private String toEventLabel(String eventType, Map<String, Object> metadata) {
        String code = trimToNull(eventType);
        if (!StringUtils.hasText(code)) {
            return "-";
        }
        return switch (code.toUpperCase()) {
            case "LOGIN_SUCCESS" -> "Login successful";
            case "LOGIN_FAILED" -> "Login failed";
            case "SESSION_REVOKED" -> "Session revoked";
            case "BACKOFFICE_CUSTOMER_STATUS_UPDATED" -> "Customer status updated";
            case "BACKOFFICE_CUSTOMER_ACCESS_BLOCKED" -> "Customer access blocked";
            case "BACKOFFICE_CUSTOMER_ACCESS_UNBLOCKED" -> "Customer access unblocked";
            case "BACKOFFICE_CUSTOMER_PASSWORD_RESET" -> "Customer password reset";
            case "BACKOFFICE_CUSTOMER_MFA_RESET" -> "Customer MFA reset";
            case "BACKOFFICE_CUSTOMER_KYC_SYNCED" -> "Customer KYC synced";
            case "BACKOFFICE_EMPLOYEE_STATUS_UPDATED" -> "Employee status updated";
            case "BACKOFFICE_EMPLOYEE_ACCESS_BLOCKED" -> "Employee access blocked";
            case "BACKOFFICE_EMPLOYEE_ACCESS_UNBLOCKED" -> "Employee access unblocked";
            case "BACKOFFICE_EMPLOYEE_PASSWORD_RESET" -> "Employee password reset";
            case "BACKOFFICE_EMPLOYEE_MFA_RESET" -> "Employee MFA reset";
            case "BACKOFFICE_EMPLOYEE_KYC_SYNCED" -> "Employee KYC synced";
            case "BACKOFFICE_ORGANIZATION_ROLE_CREATED" -> "Organization role created";
            case "BACKOFFICE_ORGANIZATION_ROLE_UPDATED" -> "Organization role updated";
            case "BACKOFFICE_ORGANIZATION_ACCESS_BLOCKED" -> "Organization access blocked";
            case "BACKOFFICE_ORGANIZATION_ACCESS_UNBLOCKED" -> "Organization access unblocked";
            case "BACKOFFICE_ORGANIZATION_KYC_SYNCED" -> "Organization KYC synced";
            default -> humanizeCode(code);
        };
    }

    private String toChannelLabel(String channel) {
        String code = trimToNull(channel);
        if (!StringUtils.hasText(code)) {
            return "-";
        }
        return switch (code.toUpperCase()) {
            case "INTERNET_BANKING", "WEB" -> "Internet Banking";
            case "MOBILE_BANKING" -> "Mobile Banking";
            case "BACKOFFICE" -> "Back Office";
            case "SMS" -> "SMS";
            case "EMAIL" -> "Email";
            case "WHATSAPP" -> "WhatsApp";
            case "PUSH" -> "Push";
            default -> humanizeCode(code);
        };
    }

    private String toEntityLabel(String entityType) {
        String code = trimToNull(entityType);
        if (!StringUtils.hasText(code)) {
            return "-";
        }
        return switch (code.toUpperCase()) {
            case ENTITY_LOGIN_HISTORY -> "Login";
            case ENTITY_SESSION_EVENT -> "Session";
            case ENTITY_SECURITY_EVENT -> "Security";
            case ENTITY_TRANSACTION -> "Transaction";
            case "CUSTOMER_PROFILE" -> "Customer Profile";
            case "EMPLOYEE_PROFILE" -> "Employee Profile";
            case ENTITY_ORGANIZATION -> "Organization";
            default -> humanizeCode(code);
        };
    }

    private String toEventDetails(BackofficeAuditTrailResponse event) {
        Map<String, Object> metadata = event.getMetadata();
        if (ENTITY_TRANSACTION.equalsIgnoreCase(event.getEntityType())) {
            return paymentDetails(
                    metadataText(metadata, "transaction_number"),
                    metadata != null ? metadata.get("amount") : null,
                    metadataText(metadata, "currency"),
                    metadataText(metadata, "status")
            );
        }
        if (ENTITY_SECURITY_EVENT.equalsIgnoreCase(event.getEntityType())) {
            return firstNonBlank(metadataText(metadata, "description"), metadataText(metadata, "severity"));
        }
        if (ENTITY_LOGIN_HISTORY.equalsIgnoreCase(event.getEntityType())
                && "LOGIN_FAILED".equalsIgnoreCase(event.getEventType())) {
            return firstNonBlank(metadataText(metadata, "failure_reason"), "Failed login attempt");
        }
        if (isSessionRevoked(event)) {
            return sessionRevokedDetails(metadata);
        }
        return firstNonBlank(
                metadataText(metadata, "actor"),
                metadataText(metadata, "status"),
                metadataText(metadata, "blocked"),
                null
        );
    }

    private String paymentEventLabel(String transactionType, String status) {
        String typeLabel = transactionTypeLabel(transactionType);
        String normalizedStatus = defaultString(status, "").toUpperCase();
        if (List.of("SUCCESS", "COMPLETED", "APPROVED").contains(normalizedStatus)) {
            return typeLabel + " completed";
        }
        if (List.of("FAILED", "REJECTED", "CANCELLED").contains(normalizedStatus)) {
            return typeLabel + " failed";
        }
        if (List.of("PENDING", "PROCESSING", "DRAFT").contains(normalizedStatus)) {
            return typeLabel + " initiated";
        }
        return typeLabel + " recorded";
    }

    private String transactionTypeLabel(String transactionType) {
        String code = trimToNull(transactionType);
        if (!StringUtils.hasText(code)) {
            return "Transaction";
        }
        return switch (code.toUpperCase()) {
            case "INTERNAL_TRANSFER" -> "Internal transfer";
            case "OWN_ACCOUNT_TRANSFER" -> "Own account transfer";
            case "IFT", "IFT_BULK" -> "IFT bulk payment";
            case "BULK", "BULK_PAYMENT" -> "Bulk payment";
            case "RTGS", "RTGS_TRANSFER" -> "RTGS transfer";
            case "SWIFT", "SWIFT_TRANSFER" -> "SWIFT transfer";
            case "EVC" -> "EVC transfer";
            case "SIPS" -> "SIPS transfer";
            default -> humanizeCode(code);
        };
    }

    private String paymentDetails(String transactionNumber, Object amount, String currency, String status) {
        List<String> parts = new ArrayList<>();
        String resolvedCurrency = defaultString(currency, "USD");
        if (amount != null) {
            parts.add(resolvedCurrency + " " + amount);
        }
        putIfPresent(parts, transactionNumber);
        putIfPresent(parts, StringUtils.hasText(status) ? humanizeCode(status) : null);
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    private void putIfPresent(List<String> values, String value) {
        if (values != null && StringUtils.hasText(value)) {
            values.add(value.trim());
        }
    }

    private String reasonLabel(String reason) {
        String code = trimToNull(reason);
        if (!StringUtils.hasText(code)) {
            return "Session closed";
        }
        return switch (code.toUpperCase()) {
            case "USER_LOGOUT" -> "User signed out";
            case "CONCURRENT_LOGIN" -> "Older session closed after new login";
            case "PASSWORD_CHANGED" -> "Password changed";
            case "REFRESH_TOKEN_REUSE" -> "Security containment after token reuse";
            case "BACKOFFICE_CUSTOMER_BLOCKED" -> "Customer blocked by back office";
            case "BACKOFFICE_CUSTOMER_PASSWORD_RESET" -> "Customer password reset";
            case "BACKOFFICE_EMPLOYEE_BLOCKED" -> "Employee blocked by back office";
            case "BACKOFFICE_EMPLOYEE_PASSWORD_RESET" -> "Employee password reset";
            case "BACKOFFICE_ORGANIZATION_BLOCKED" -> "Organization blocked by back office";
            case "BACKOFFICE_ORGANIZATION_USER_BLOCKED" -> "Organization user blocked";
            case "BACKOFFICE_ORGANIZATION_USER_PASSWORD_RESET" -> "Organization user password reset";
            case "IMPOSSIBLE_TRAVEL" -> "Security revocation after impossible travel";
            case "REFRESH_DEVICE_MISMATCH" -> "Security revocation after device mismatch";
            default -> humanizeCode(code);
        };
    }

    private String maskDeviceId(String deviceId) {
        String normalized = trimToNull(deviceId);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() <= 8 ? normalized : normalized.substring(normalized.length() - 8);
    }

    private String humanizeCode(String code) {
        String normalized = trimToNull(code);
        if (!StringUtils.hasText(normalized)) {
            return "-";
        }
        String[] words = normalized.replace('-', '_').replace(' ', '_').toLowerCase().split("_+");
        List<String> labels = new ArrayList<>();
        for (String word : words) {
            if (!StringUtils.hasText(word)) {
                continue;
            }
            labels.add(switch (word) {
                case "mfa" -> "MFA";
                case "kyc" -> "KYC";
                case "rtgs" -> "RTGS";
                case "swift" -> "SWIFT";
                case "ift" -> "IFT";
                case "sms" -> "SMS";
                case "id" -> "ID";
                default -> word.substring(0, 1).toUpperCase() + word.substring(1);
            });
        }
        return labels.isEmpty() ? normalized : String.join(" ", labels);
    }

    private String metadataText(Map<String, Object> metadata, String key) {
        if (metadata == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = metadata.get(key);
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof java.time.Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
        }
        return null;
    }

    private String resolveUserCategory(IamUserEntity iamUser) {
        if (iamUser == null) {
            return null;
        }
        if (iamUser.getEmployeeProfile() != null) {
            return "EMPLOYEE";
        }
        if (iamUser.getCustomerProfile() != null) {
            return "CUSTOMER";
        }
        if (iamUser.getOrganizationUsers() != null && !iamUser.getOrganizationUsers().isEmpty()) {
            return "CORPORATE";
        }
        return null;
    }

    private Map<String, Object> withActor(Map<String, Object> metadata) {
        Map<String, Object> result = copyMetadata(metadata);
        String actor = currentActor();
        if (StringUtils.hasText(actor)) {
            result.put("actor", actor);
        }
        return result.isEmpty() ? null : result;
    }

    private void applyCurrentRequestContext(IamAuditLogEntity entry) {
        if (entry == null) {
            return;
        }
        try {
            RequestContextExtractor.RequestContext context = requestContextExtractor.extractContext();
            if (context == null) {
                return;
            }
            entry.setIpAddress(trimToNull(context.getIpAddress()));
            entry.setDeviceId(trimToNull(context.getDeviceId()));
            entry.setUserAgent(trimToNull(context.getUserAgent()));
        } catch (Exception exception) {
            log.debug("Unable to attach request context to backoffice audit log: {}", exception.getMessage());
        }
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        return StringUtils.hasText(name) && !"anonymousUser".equalsIgnoreCase(name) ? name : null;
    }

    private Map<String, Object> copyMetadata(Map<String, Object> metadata) {
        Map<String, Object> result = newMetadata();
        if (metadata != null) {
            metadata.forEach((key, value) -> putIfPresent(result, key, value));
        }
        return result;
    }

    private Map<String, Object> newMetadata() {
        return new LinkedHashMap<>();
    }

    private void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (metadata == null || !StringUtils.hasText(key) || value == null) {
            return;
        }
        if (value instanceof String text && !StringUtils.hasText(text)) {
            return;
        }
        metadata.put(key, value);
    }

    private String defaultString(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : trimToNull(fallback);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record NetworkContext(OffsetDateTime createdAt, String ipAddress, String deviceId) {
    }
}
