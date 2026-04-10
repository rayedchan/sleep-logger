package com.noom.interview.fullstack.sleep.util
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetTime
import java.time.ZoneOffset

class TimeAveragingEngineTest {
    private fun time(h: Int, m: Int): OffsetTime =
        OffsetTime.of(h, m, 0, 0, ZoneOffset.UTC)

    private fun sec(h: Int, m: Int): Double =
        (h * 3600 + m * 60).toDouble()


    @Test
    fun `averages simple linear times`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(11, 0), sec(13, 0))
        )

        assertEquals(time(12, 0), result)
    }

    @Test
    fun `averages multiple linear times`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(3, 0), sec(5, 0), sec(7, 0))
        )

        assertEquals(time(5, 0), result)
    }

    // -----------------------------
    // 🌙 Midnight wrap cases
    // -----------------------------

    @Test
    fun `averages times across midnight`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(23, 0), sec(1, 0))
        )

        assertEquals(time(0, 0), result)
    }

    @Test
    fun `averages clustered midnight times`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(23, 30), sec(0, 30), sec(1, 0))
        )

        assertEquals(time(0, 20), result) // ~00:20
    }

    @Test
    fun `handles exactly 12 hours apart`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(22, 0), sec(10, 0))
        )

        assertEquals(time(4, 0), result)
    }

    @Test
    fun `handles noon and midnight symmetry`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(0, 0), sec(12, 0))
        )

        assertEquals(time(18, 0), result)
    }

    @Test
    fun `handles trig cancellation with multiple symmetric points`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(0, 0), sec(6, 0), sec(12, 0), sec(18, 0))
        )

        // median fallback → 09:00
        assertEquals(time(9, 0), result)
    }

    @Test
    fun `returns null for empty input`() {
        val result = TimeAveragingEngine.average(emptyList())
        assertEquals(null, result)
    }

    @Test
    fun `returns same value for single input`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(8, 30))
        )

        assertEquals(time(8, 30), result)
    }

    @Test
    fun `real sleep pattern`() {
        val result = TimeAveragingEngine.average(
            listOf(sec(23, 0), sec(0, 30), sec(1, 0))
        )

        assertEquals(time(0, 10), result) // ~00:10
    }
}