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

    public List<WeeklyTrendRow> findWeeklyLoggedInUsers(LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return jdbcTemplate.query(WEEKLY_LOGGED_IN_USERS_SQL, params, (resultSet, _rowNum) -> new WeeklyTrendRow(
                resultSet.getDate("day").toLocalDate(),
                resultSet.getLong("value")
        ));
    }

    public record WeeklyTrendRow(LocalDate day, long value) {
    }
}
