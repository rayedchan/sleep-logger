package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import com.noom.interview.fullstack.sleep.entity.SleepQuality
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class SleepRepository(private val jdbcTemplate: JdbcTemplate) {

    private val rowMapper = { rs: ResultSet, _: Int -> SleepLogEntity(
        id = rs.getInt("id"),
        userId = UUID.fromString(rs.getString("user_id")),
        logDate = rs.getDate("log_date").toLocalDate(),
        bedTime = rs.getObject("bed_time", java.time.OffsetDateTime::class.java),
        wakeTime = rs.getObject("wake_time", java.time.OffsetDateTime::class.java),
        mood = SleepQuality.valueOf(rs.getString("mood")),
        totalDurationSeconds = rs.getLong("total_duration_seconds")
    ) }

    fun saveSleepLog(userId: UUID, log: SleepLogEntity): SleepLogEntity {
        val sql = """
            INSERT INTO sleep_logs (user_id, log_date, bed_time, wake_time, mood)
            VALUES (?, ?, ?, ?, ?::sleep_quality)
            RETURNING id, user_id, log_date, bed_time, wake_time, mood, EXTRACT(EPOCH FROM total_duration)::bigint as total_duration_seconds
        """

        return jdbcTemplate.queryForObject(sql, rowMapper, userId, log.logDate, log.bedTime, log.wakeTime, log.mood.name)!!
    }

    fun findMostRecentSleepLog(userId: UUID): SleepLogEntity? {
        val sql = """
            SELECT 
                id, user_id, log_date, bed_time, wake_time, mood, 
                EXTRACT(EPOCH FROM total_duration)::bigint as total_duration_seconds
            FROM sleep_logs WHERE user_id = ? 
            ORDER BY log_date DESC LIMIT 1
        """

        return jdbcTemplate.query(sql, rowMapper, userId).firstOrNull()
    }

    fun getAggregatedStats(userId: UUID, days: Int): Map<String, Any?> {
        val sql = """
            SELECT 
                -- 1. Average Duration (Internal interval calculation)
                EXTRACT(EPOCH FROM AVG(total_duration)) as avg_seconds,
            
                -- 2. Average Bed Time (Circular-aware & UTC-locked)
                (((AVG(
                    CASE 
                        WHEN EXTRACT(HOUR FROM (bed_time AT TIME ZONE 'UTC')) < 12 
                        THEN EXTRACT(EPOCH FROM (bed_time AT TIME ZONE 'UTC')::time) + 86400
                        ELSE EXTRACT(EPOCH FROM (bed_time AT TIME ZONE 'UTC')::time)
                    END
                )::bigint % 86400) * INTERVAL '1 second')::time::text || 'Z') as avg_start,
            
                -- 3. Average Wake Time (Circular-aware & UTC-locked)
                (((AVG(
                    CASE 
                        WHEN EXTRACT(HOUR FROM (wake_time AT TIME ZONE 'UTC')) < 12 
                        THEN EXTRACT(EPOCH FROM (wake_time AT TIME ZONE 'UTC')::time) + 86400
                        ELSE EXTRACT(EPOCH FROM (wake_time AT TIME ZONE 'UTC')::time)
                    END
                )::bigint % 86400) * INTERVAL '1 second')::time::text || 'Z') as avg_end,
            
                -- 4. Frequencies and Range
                COUNT(*) FILTER (WHERE mood = 'GOOD') as count_good,
                COUNT(*) FILTER (WHERE mood = 'OK') as count_ok,
                COUNT(*) FILTER (WHERE mood = 'BAD') as count_bad,
                MIN(log_date) as range_start,
                MAX(log_date) as range_end
            FROM sleep_logs 
            WHERE user_id = ? 
              AND log_date > CURRENT_DATE - CAST(? || ' days' AS INTERVAL);
        """
        return jdbcTemplate.queryForMap(sql, userId, days)
    }
}