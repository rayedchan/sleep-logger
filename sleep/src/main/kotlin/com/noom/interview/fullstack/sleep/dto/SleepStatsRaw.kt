package com.noom.interview.fullstack.sleep.dto

import java.time.LocalDate

data class SleepStatsRaw(
    val avgSeconds: Long?,
    val bedTimes: List<Double>,
    val wakeTimes: List<Double>,
    val countGood: Int,
    val countOk: Int,
    val countBad: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?
)
