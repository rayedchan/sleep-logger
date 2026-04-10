package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.dto.SleepStatsRaw
import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import com.noom.interview.fullstack.sleep.entity.SleepQuality
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class SleepRepository(private val jdbcTemplate: JdbcTemplate) {

    private val sleepLogRowMapper = { rs: ResultSet, _: Int -> SleepLogEntity(
        id = rs.getInt("id"),
        userId = UUID.fromString(rs.getString("user_id")),
        logDate = rs.getDate("log_date").toLocalDate(),
        bedTime = rs.getObject("bed_time", java.time.OffsetDateTime::class.java),
        wakeTime = rs.getObject("wake_time", java.time.OffsetDateTime::class.java),
        mood = SleepQuality.valueOf(rs.getString("mood")),
        totalDurationSeconds = rs.getLong("total_duration_seconds")
    ) }

    private val statsRowMapper = RowMapper { rs: ResultSet, _ ->
        val bedArray = rs.getArray("bed_times")?.array as? Array<*>
        val wakeArray = rs.getArray("wake_times")?.array as? Array<*>

        val bedTimes = bedArray
            ?.mapNotNull { (it as? Number)?.toDouble() }
            ?: emptyList()

        val wakeTimes = wakeArray
            ?.mapNotNull { (it as? Number)?.toDouble() }
            ?: emptyList()

        SleepStatsRaw(
            avgSeconds = rs.getLong("avg_seconds").takeUnless { rs.wasNull() },
            bedTimes = bedTimes,
            wakeTimes = wakeTimes,
            countGood = rs.getInt("count_good"),
            countOk = rs.getInt("count_ok"),
            countBad = rs.getInt("count_bad"),
            startDate = rs.getDate("range_start")?.toLocalDate(),
            endDate = rs.getDate("range_end")?.toLocalDate()
        )
    }

    fun saveSleepLog(userId: UUID, log: SleepLogEntity): SleepLogEntity {
        val sql = """
            INSERT INTO sleep_logs (user_id, log_date, bed_time, wake_time, mood)
            VALUES (?, ?, ?, ?, ?::sleep_quality)
            RETURNING id, user_id, log_date, bed_time, wake_time, mood, EXTRACT(EPOCH FROM total_duration)::bigint as total_duration_seconds
        """

        return jdbcTemplate.queryForObject(sql, sleepLogRowMapper, userId, log.logDate, log.bedTime, log.wakeTime, log.mood.name)!!
    }

    fun findMostRecentSleepLog(userId: UUID): SleepLogEntity? {
        val sql = """
            SELECT 
                id, user_id, log_date, bed_time, wake_time, mood, 
                EXTRACT(EPOCH FROM total_duration)::bigint as total_duration_seconds
            FROM sleep_logs WHERE user_id = ? 
            ORDER BY log_date DESC LIMIT 1
        """

        return jdbcTemplate.query(sql, sleepLogRowMapper, userId).firstOrNull()
    }

    fun getDetermininisticAggregatedStats(userId: UUID, days: Int): SleepStatsRaw {
        val sql = """
            SELECT 
                -- 1. Average Duration
                EXTRACT(EPOCH FROM AVG(total_duration))::bigint AS avg_seconds,
            
                -- 2. Raw time arrays 
                ARRAY_AGG(EXTRACT(EPOCH FROM (bed_time AT TIME ZONE 'UTC')::time))  AS bed_times,
                ARRAY_AGG(EXTRACT(EPOCH FROM (wake_time AT TIME ZONE 'UTC')::time)) AS wake_times,
            
                -- 3. Frequencies
                COUNT(*) FILTER (WHERE mood = 'GOOD') AS count_good,
                COUNT(*) FILTER (WHERE mood = 'OK')   AS count_ok,
                COUNT(*) FILTER (WHERE mood = 'BAD')  AS count_bad,
            
                -- 4. Date range
                MIN(log_date) AS range_start,
                MAX(log_date) AS range_end
            
            FROM sleep_logs
            WHERE user_id = ?
              AND log_date >= CURRENT_DATE - (? * INTERVAL '1 day');
        """

        return jdbcTemplate.queryForObject(sql, statsRowMapper,userId, days)!!
    }
}