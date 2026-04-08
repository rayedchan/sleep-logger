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
}