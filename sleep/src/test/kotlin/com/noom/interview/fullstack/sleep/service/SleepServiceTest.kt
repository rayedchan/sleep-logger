package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.entity.SleepLogEntity
import com.noom.interview.fullstack.sleep.entity.SleepQuality
import com.noom.interview.fullstack.sleep.mapper.toEntity
import com.noom.interview.fullstack.sleep.repository.SleepRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID


class SleepServiceTest {
    private val sleepRepository: SleepRepository = mock(SleepRepository::class.java)
    private val sleepService = SleepService(sleepRepository)

    @Test
    fun `createSleepLog should throw exception when bedTime is after wakeTime`() {
        // Arrange
        val request = CreateSleepLogRequest(
            logDate = LocalDate.now(),
            bedTime = OffsetDateTime.parse("2026-04-08T08:00:00Z"),
            wakeTime = OffsetDateTime.parse("2026-04-08T06:00:00Z"), // Earlier!
            mood = SleepQuality.OK
        )

        // Act & Assert
        val exception = assertThrows<ResponseStatusException> {
            sleepService.createSleepLog(UUID.randomUUID(), request)
        }
        assertEquals("Bed time cannot be after wake time", exception.reason)

        // Verify the repository was NEVER called (since validation failed)
        verifyNoInteractions(sleepRepository)
    }

    @Test
    fun `createSleepLog should throw exception when duration is 24 hours or more`() {
        // Arrange
        val request = CreateSleepLogRequest(
            logDate = LocalDate.now(),
            bedTime = OffsetDateTime.parse("2026-04-08T06:00:00Z"),
            wakeTime = OffsetDateTime.parse("2026-04-09T06:00:00Z"), // Exactly 24h
            mood = SleepQuality.OK
        )

        // Act & Assert
        val exception = assertThrows<ResponseStatusException> {
            sleepService.createSleepLog(UUID.randomUUID(), request)
        }
        assertTrue(exception.reason!!.contains("cannot exceed 24 hours"))
    }

    @Test
    fun `createSleepLog should call repository when data is valid`() {
        val userId = UUID.randomUUID()
        val request = CreateSleepLogRequest(
            logDate = LocalDate.now(),
            bedTime = OffsetDateTime.parse("2026-04-08T22:00:00Z"),
            wakeTime = OffsetDateTime.parse("2026-04-09T06:00:00Z"),
            mood = SleepQuality.GOOD
        )

        val mockEntity = request.toEntity(userId)

        val mockEntityReturn = SleepLogEntity(
            id = 1,
            userId = userId,
            logDate = request.logDate,
            bedTime = request.bedTime,
            wakeTime = request.wakeTime,
            mood = request.mood,
            totalDurationSeconds = 28800
        )

        doReturn(mockEntityReturn)
            .`when`(sleepRepository)
            .saveSleepLog(userId, mockEntity)

        val result = sleepService.createSleepLog(userId, request)

        assertEquals(28800, result.totalDurationSeconds)

        verify(sleepRepository).saveSleepLog(userId, mockEntity)
    }
}
