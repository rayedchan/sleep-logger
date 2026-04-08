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
        totalDuration = rs.getString("total_duration")
    ) }

    fun saveSleepLog(userId: UUID, log: SleepLogEntity): SleepLogEntity {
        val sql = """
            INSERT INTO sleep_logs (user_id, log_date, bed_time, wake_time, mood)
            VALUES (?, ?, ?, ?, ?::sleep_quality)
            RETURNING id, user_id, log_date, bed_time, wake_time, mood, total_duration
        """

        return jdbcTemplate.queryForObject(sql, rowMapper, userId, log.logDate, log.bedTime, log.wakeTime, log.mood.name)!!
    }

    fun findMostRecentSleepLog(userId: UUID): SleepLogEntity? {
        val sql = """
            SELECT * FROM sleep_logs WHERE user_id = ? 
            ORDER BY log_date DESC LIMIT 1
        """

        return jdbcTemplate.query(sql, rowMapper, userId).firstOrNull()
    }

    fun getAggregatedStats(userId: UUID, days: Int): Map<String, Any?> {
        val sql = """
            SELECT 
                EXTRACT(EPOCH FROM AVG(total_duration)) AS avg_seconds,
                
                TO_CHAR(
                    (
                        AVG(
                            EXTRACT(EPOCH FROM (
                                CASE 
                                    WHEN bed_time::time < TIME '12:00'
                                        THEN bed_time + INTERVAL '24 hours'
                                    ELSE bed_time
                                END
                            ))
                        ) * INTERVAL '1 second'
                    )::time,
                    'HH24:MI:SS'
                ) AS avg_start,
                
                TO_CHAR(
                    (
                        AVG(
                            EXTRACT(EPOCH FROM (
                                CASE 
                                    WHEN wake_time::time < TIME '12:00'
                                        THEN wake_time + INTERVAL '24 hours'
                                    ELSE wake_time
                                END
                            ))
                        ) * INTERVAL '1 second'
                    )::time,
                    'HH24:MI:SS'
                ) AS avg_end,
                
                COUNT(*) FILTER (WHERE mood = 'GOOD') AS count_good,
                COUNT(*) FILTER (WHERE mood = 'OK')   AS count_ok,
                COUNT(*) FILTER (WHERE mood = 'BAD')  AS count_bad,
                
                MIN(log_date) AS range_start,
                MAX(log_date) AS range_end
            FROM sleep_logs 
            WHERE user_id = ?
              AND log_date > CURRENT_DATE - CAST(? || ' days' AS INTERVAL);
        """
        return jdbcTemplate.queryForMap(sql, userId, days)
    }
}