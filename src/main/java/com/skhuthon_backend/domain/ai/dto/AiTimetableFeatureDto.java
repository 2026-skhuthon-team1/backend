package com.skhuthon_backend.domain.ai.dto;

import lombok.Builder;

import java.time.LocalTime;

@Builder
public record AiTimetableFeatureDto(

        Integer attendanceDays,

        LocalTime earliestStartTime,

        LocalTime latestEndTime,

        Integer firstPeriodCount,

        Integer longestBreakMinutes,

        Integer longBreakCount,

        Integer lunchBreakCount,

        Integer earlyFinishDayCount

) {
}
