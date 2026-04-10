package com.noom.interview.fullstack.sleep.util

import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

object TimeAveragingEngine {
    private const val DAY = 86400.0
    private const val HALF_DAY = DAY / 2
    private const val QUARTER_DAY = DAY / 4

    fun average(values: List<Double>): OffsetTime? {
        if (values.isEmpty()) return null
        if (values.size == 1) return toOffsetTime(values.first())

        val linearMean = values.average()
        val circularMean = circularMean(values)
        val magnitude = vectorMagnitude(values)

        val spread = values.maxOrNull()!! - values.minOrNull()!!
        val midnightDensity = values.count { it < 7200 || it > 79200 } / values.size.toDouble()

        val isCircular = magnitude > 0.35 && midnightDensity > 0.3 && spread > 18000

        val chosen = when {
            magnitude < 0.1 -> {
                if (values.size == 2) {
                    midpointCircular(values[0], values[1])
                } else {
                    median(values)
                }
            }
            isCircular -> circularMean
            else -> linearMean
        }

        return toOffsetTime(chosen)
    }

    private fun circularMean(values: List<Double>): Double {
        val avgSin = values.map { sin(it * 2 * PI / DAY) }.average()
        val avgCos = values.map { cos(it * 2 * PI / DAY) }.average()
        return ((atan2(avgSin, avgCos) * DAY / (2 * PI)) + DAY) % DAY
    }

    private fun vectorMagnitude(values: List<Double>): Double {
        val avgSin = values.map { sin(it * 2 * PI / DAY) }.average()
        val avgCos = values.map { cos(it * 2 * PI / DAY) }.average()
        return sqrt(avgSin * avgSin + avgCos * avgCos)
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 0) {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        } else {
            sorted[n / 2]
        }
    }

    private fun midpointCircular(a: Double, b: Double): Double {
        val diff = kotlin.math.abs(a - b)

        if (diff == HALF_DAY) {
            val earlier = minOf(a, b)
            return (earlier + 3 * QUARTER_DAY) % DAY
        }

        val delta = ((b - a + DAY + HALF_DAY) % DAY) - HALF_DAY
        return (a + delta / 2 + DAY) % DAY
    }

    private fun toOffsetTime(seconds: Double): OffsetTime {
        val normalized = ((seconds % DAY) + DAY) % DAY
        val rounded = round(normalized / 60) * 60

        val hours = (rounded / 3600).toInt()
        val minutes = ((rounded % 3600) / 60).toInt()

        return OffsetTime.of(hours, minutes, 0, 0, ZoneOffset.UTC)
    }
}