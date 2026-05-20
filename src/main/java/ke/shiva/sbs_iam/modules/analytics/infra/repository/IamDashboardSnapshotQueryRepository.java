package ke.shiva.sbs_iam.modules.analytics.infra.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class IamDashboardSnapshotQueryRepository {

    private static final String COUNT_TOTAL_USERS_SQL = """
            SELECT COUNT(1)
            FROM iam_service.iam_user
            """;

    private static final String COUNT_ONLINE_USERS_SQL = """
            SELECT COUNT(1)
            FROM iam_service.sessions s
            WHERE s.session_type = 'LOGIN_ACTIVE'
              AND s.revoked_at IS NULL
              AND COALESCE(s.expires_at, NOW() + INTERVAL '365 days') > NOW()
              AND COALESCE(s.last_access_at, s.created_at) >= (NOW() - (:activeWindowMinutes * INTERVAL '1 minute'))
            """;

    private static final String COUNT_FAILED_LOGINS_SQL = """
            SELECT COUNT(1)
            FROM iam_service.login_history lh
            WHERE lh.created_at >= :since
              AND lh.success = FALSE
              AND COALESCE(lh.failure_reason, '') NOT IN ('PENDING_PASSWORD_AUTH', 'PENDING_MFA', 'PENDING_FINALIZATION')
            """;

    private static final String COUNT_LOCKED_ACCOUNTS_SQL = """
            SELECT
                COALESCE((SELECT COUNT(1) FROM iam_service.customer_auth ca WHERE ca.internet_locked = TRUE OR ca.mobile_locked = TRUE), 0)
              + COALESCE((SELECT COUNT(1) FROM iam_service.employee_auth ea WHERE ea.staff_locked = TRUE), 0)
              + COALESCE((SELECT COUNT(1) FROM iam_service.organization org WHERE org.account_locked = TRUE), 0)
            """;

    private static final String WEEKLY_LOGGED_IN_USERS_SQL = """
            WITH days AS (
                SELECT generate_series(CAST(:startDate AS DATE), CAST(:endDate AS DATE), INTERVAL '1 day')::date AS day
            )
            SELECT
                d.day AS day,
                COALESCE(COUNT(DISTINCT lh.iam_user_id), 0) AS value
            FROM days d
            LEFT JOIN iam_service.login_history lh
                   ON lh.success = TRUE
                  AND lh.created_at >= d.day
                  AND lh.created_at < d.day + INTERVAL '1 day'
            GROUP BY d.day
            ORDER BY d.day
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public IamDashboardSnapshotQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countTotalUsers() {
        Long result = jdbcTemplate.queryForObject(COUNT_TOTAL_USERS_SQL, new MapSqlParameterSource(), Long.class);
        return result == null ? 0L : result;
    }

    public long countOnlineUsers(int activeWindowMinutes) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("activeWindowMinutes", Math.max(activeWindowMinutes, 1));
        Long result = jdbcTemplate.queryForObject(COUNT_ONLINE_USERS_SQL, params, Long.class);
        return result == null ? 0L : result;
    }

    public long countFailedLoginsSince(OffsetDateTime since) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("since", since);
        Long result = jdbcTemplate.queryForObject(COUNT_FAILED_LOGINS_SQL, params, Long.class);
        return result == null ? 0L : result;
    }

    public long countLockedAccounts() {
        Long result = jdbcTemplate.queryForObject(COUNT_LOCKED_ACCOUNTS_SQL, new MapSqlParameterSource(), Long.class);
        return result == null ? 0L : result;
    }

    public List<AccessMetricRow> findPlatformAccessMetrics(OffsetDateTime now) {
        return List.of(
                channelAccessMetric(
                        "EBANKING",
                        "eBanking Users",
                        "INTERNET_BANKING",
                        "internet_password_hash",
                        "internet_locked",
                        "internet_lockout_until",
                        "Active access",
                        "Configured",
                        "Password set",
                        "Locked",
                        "monitor",
                        now
                ),
                channelAccessMetric(
                        "MBANKING",
                        "mBanking Users",
                        "MOBILE_BANKING",
                        "mobile_pin_hash",
                        "mobile_locked",
                        "mobile_lockout_until",
                        "Active access",
                        "Configured",
                        "PIN set",
                        "Locked",
                        "phone",
                        now
                ),
                employeeAccessMetric(now),
                organizationAccessMetric()
        );
    }

    public List<WeeklyTrendRow> findWeeklyLoggedInUsers(LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return jdbcTemplate.query(WEEKLY_LOGGED_IN_USERS_SQL, params, (resultSet, _rowNum) -> new WeeklyTrendRow(
                resultSet.getDate("day").toLocalDate(),
                resultSet.getLong("value")
        ));
    }

    private AccessMetricRow channelAccessMetric(String key,
                                                String label,
                                                String channel,
                                                String credentialColumn,
                                                String lockedColumn,
                                                String lockoutColumn,
                                                String activeLabel,
                                                String configuredLabel,
                                                String credentialsLabel,
                                                String lockedLabel,
                                                String icon,
                                                OffsetDateTime now) {
        String baseFrom = """
                FROM iam_service.customer_profile cp
                JOIN iam_service.iam_user iu ON iu.id = cp.iam_user_id
                LEFT JOIN iam_service.customer_auth ca ON ca.iam_user_id = cp.iam_user_id
                WHERE COALESCE(cp.is_verified, false) = true
                """;
        String hasChannelIdentifier = """
                AND EXISTS (
                    SELECT 1
                    FROM iam_service.login_identifier li
                    WHERE li.iam_user_id = cp.iam_user_id
                      AND li.channel = :channel
                )
                """;
        String hasActiveChannelIdentifier = """
                AND EXISTS (
                    SELECT 1
                    FROM iam_service.login_identifier li
                    WHERE li.iam_user_id = cp.iam_user_id
                      AND li.channel = :channel
                      AND li.status = 'ACTIVE'
                )
                """;
        String credentialSet = " AND TRIM(COALESCE(ca." + credentialColumn + ", '')) <> ''";
        String locked = " AND (COALESCE(ca." + lockedColumn + ", false) = true OR ca." + lockoutColumn + " > :now)";
        String notLocked = " AND COALESCE(ca." + lockedColumn + ", false) = false"
                + " AND (ca." + lockoutColumn + " IS NULL OR ca." + lockoutColumn + " <= :now)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("channel", channel)
                .addValue("now", now);

        long configured = scalar("SELECT COUNT(DISTINCT cp.iam_user_id) " + baseFrom + hasChannelIdentifier, params);
        long credentialsSet = scalar("SELECT COUNT(DISTINCT cp.iam_user_id) " + baseFrom + credentialSet, params);
        long lockedCount = scalar("SELECT COUNT(DISTINCT cp.iam_user_id) " + baseFrom + locked, params);
        long active = scalar(
                "SELECT COUNT(DISTINCT cp.iam_user_id) " + baseFrom
                        + " AND iu.status = 'ACTIVE'"
                        + hasActiveChannelIdentifier
                        + credentialSet
                        + notLocked,
                params
        );

        return new AccessMetricRow(
                key,
                label,
                active,
                activeLabel,
                configured,
                configuredLabel,
                credentialsSet,
                credentialsLabel,
                lockedCount,
                lockedLabel,
                icon
        );
    }

    private AccessMetricRow employeeAccessMetric(OffsetDateTime now) {
        String baseFrom = """
                FROM iam_service.employee_profile ep
                JOIN iam_service.iam_user iu ON iu.id = ep.iam_user_id
                LEFT JOIN iam_service.employee_auth ea ON ea.iam_user_id = ep.iam_user_id
                WHERE 1 = 1
                """;
        String credentialSet = " AND TRIM(COALESCE(ea.staff_password_hash, '')) <> ''";
        String lockedOrDisabled = """
                AND (
                    COALESCE(ea.staff_locked, false) = true
                    OR ea.staff_lockout_until > :now
                    OR COALESCE(ep.employment_status, '') <> 'ACTIVE'
                    OR COALESCE(iu.status, '') <> 'ACTIVE'
                )
                """;
        String activeAccess = """
                AND COALESCE(iu.status, '') = 'ACTIVE'
                AND COALESCE(ep.employment_status, '') = 'ACTIVE'
                AND COALESCE(ea.staff_locked, false) = false
                AND (ea.staff_lockout_until IS NULL OR ea.staff_lockout_until <= :now)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("now", now);

        long configured = scalar("SELECT COUNT(DISTINCT ep.iam_user_id) " + baseFrom, params);
        long credentialsSet = scalar("SELECT COUNT(DISTINCT ep.iam_user_id) " + baseFrom + credentialSet, params);
        long locked = scalar("SELECT COUNT(DISTINCT ep.iam_user_id) " + baseFrom + lockedOrDisabled, params);
        long active = scalar(
                "SELECT COUNT(DISTINCT ep.iam_user_id) " + baseFrom + credentialSet + activeAccess,
                params
        );

        return new AccessMetricRow(
                "EMPLOYEES",
                "Employees",
                active,
                "Active access",
                configured,
                "Staff total",
                credentialsSet,
                "Password set",
                locked,
                "Locked/disabled",
                "briefcase"
        );
    }

    private AccessMetricRow organizationAccessMetric() {
        long configured = scalar("""
                SELECT COUNT(1)
                FROM iam_service.organization org
                """, new MapSqlParameterSource());
        long active = scalar("""
                SELECT COUNT(1)
                FROM iam_service.organization org
                WHERE COALESCE(org.account_locked, false) = false
                """, new MapSqlParameterSource());
        long companyUsers = scalar("""
                SELECT COUNT(1)
                FROM iam_service.organization_user ou
                WHERE COALESCE(ou.status, '') = 'ACTIVE'
                """, new MapSqlParameterSource());
        long locked = scalar("""
                SELECT COUNT(1)
                FROM iam_service.organization org
                WHERE COALESCE(org.account_locked, false) = true
                """, new MapSqlParameterSource());

        return new AccessMetricRow(
                "ORGANIZATIONS",
                "Organizations",
                active,
                "Active orgs",
                configured,
                "Total orgs",
                companyUsers,
                "Company users",
                locked,
                "Locked orgs",
                "building"
        );
    }

    private long scalar(String sql, MapSqlParameterSource params) {
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result == null ? 0L : result;
    }

    public record WeeklyTrendRow(LocalDate day, long value) {
    }

    public record AccessMetricRow(
            String key,
            String label,
            long active,
            String activeLabel,
            long configured,
            String configuredLabel,
            long credentialsSet,
            String credentialsLabel,
            long locked,
            String lockedLabel,
            String icon
    ) {
    }
}
