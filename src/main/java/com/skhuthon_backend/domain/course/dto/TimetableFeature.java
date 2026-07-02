package com.skhuthon_backend.domain.course.dto;

import java.time.LocalTime;

public record TimetableFeature(
        int attendanceDays,
        LocalTime earliestStartTime,
        LocalTime latestEndTime,
        int firstPeriodCount,
        int longestBreakMinutes,
        int longBreakCount,
        int lunchBreakCount,
        int earlyFinishDayCount
) {
    /**
     * Signature 기반 중복 제거용 Key
     */
    public String signature() {
        return attendanceDays
                + "|"
                + earliestStartTime
                + "|"
                + latestEndTime
                + "|"
                + firstPeriodCount
                + "|"
                + longestBreakMinutes
                + "|"
                + longBreakCount
                + "|"
                + lunchBreakCount
                + "|"
                + earlyFinishDayCount;
    }
}
