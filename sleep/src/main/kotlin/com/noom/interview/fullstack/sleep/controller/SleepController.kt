package com.noom.interview.fullstack.sleep.controller

import com.noom.interview.fullstack.sleep.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.dto.SleepLogDto
import com.noom.interview.fullstack.sleep.service.SleepService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/sleep")
class SleepController(private val sleepService: SleepService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun logSleep(@RequestHeader("X-User-Id") userId: UUID, @RequestBody request: CreateSleepLogRequest): SleepLogDto {
        return sleepService.createSleepLog(userId, request)
    }

    @GetMapping("/last-night")
    fun getLastNight(@RequestHeader("X-User-Id") userId: UUID): SleepLogDto? {
        return sleepService.getMostRecentSleepLog(userId);
    }




}