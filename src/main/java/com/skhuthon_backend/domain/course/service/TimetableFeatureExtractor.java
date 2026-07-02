package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.dto.TimetableFeature;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TimetableFeatureExtractor {

    private static final LocalTime FIRST_PERIOD = LocalTime.of(9, 0);

    private static final LocalTime LUNCH_START = LocalTime.of(11, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    private static final LocalTime EARLY_FINISH_TIME = LocalTime.of(15, 0);

    private static final int LONG_BREAK_MINUTES = 120;

    public TimetableFeature extract(TimetableCombination combination) {

        List<OfferingTime> times = combination.times();

        if (times.isEmpty()) {
            return new TimetableFeature(
                    0,
                    null,
                    null,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }

        Map<DayOfWeek, List<OfferingTime>> timesByDay = times.stream()
                .collect(Collectors.groupingBy(OfferingTime::getDayOfWeek));

        return new TimetableFeature(
                calculateAttendanceDays(timesByDay),
                calculateEarliestStart(times),
                calculateLatestEnd(times),
                calculateFirstPeriodCount(times),
                calculateLongestBreak(timesByDay),
                calculateLongBreakCount(timesByDay),
                calculateLunchBreakCount(timesByDay),
                calculateEarlyFinishDayCount(timesByDay)
        );
    }

    private int calculateAttendanceDays(
            Map<DayOfWeek, List<OfferingTime>> timesByDay
    ) {
        return timesByDay.size();
    }

    private LocalTime calculateEarliestStart(
            List<OfferingTime> times
    ) {
        return times.stream()
                .map(OfferingTime::getStartTime)
                .min(LocalTime::compareTo)
                .orElse(null);
    }

    private LocalTime calculateLatestEnd(
            List<OfferingTime> times
    ) {
        return times.stream()
                .map(OfferingTime::getEndTime)
                .max(LocalTime::compareTo)
                .orElse(null);
    }

    private int calculateFirstPeriodCount(
            List<OfferingTime> times
    ) {
        return (int) times.stream()
                .filter(time -> FIRST_PERIOD.equals(time.getStartTime()))
                .count();
    }

    /**
     * 가장 긴 공강(분)
     */
    private int calculateLongestBreak(
            Map<DayOfWeek, List<OfferingTime>> timesByDay
    ) {

        int longest = 0;

        for (List<OfferingTime> dayTimes : timesByDay.values()) {

            List<OfferingTime> sorted = dayTimes.stream()
                    .sorted(Comparator.comparing(OfferingTime::getStartTime))
                    .toList();

            for (int i = 0; i < sorted.size() - 1; i++) {

                int gap = (int) Duration.between(
                        sorted.get(i).getEndTime(),
                        sorted.get(i + 1).getStartTime()
                ).toMinutes();

                longest = Math.max(longest, gap);
            }
        }

        return longest;
    }

    /**
     * 우주공강 개수 (120분 이상)
     */
    private int calculateLongBreakCount(
            Map<DayOfWeek, List<OfferingTime>> timesByDay
    ) {

        int count = 0;

        for (List<OfferingTime> dayTimes : timesByDay.values()) {

            List<OfferingTime> sorted = dayTimes.stream()
                    .sorted(Comparator.comparing(OfferingTime::getStartTime))
                    .toList();

            for (int i = 0; i < sorted.size() - 1; i++) {

                int gap = (int) Duration.between(
                        sorted.get(i).getEndTime(),
                        sorted.get(i + 1).getStartTime()
                ).toMinutes();

                if (gap >= LONG_BREAK_MINUTES) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 점심시간 확보한 요일 수
     */
    private int calculateLunchBreakCount(
            Map<DayOfWeek, List<OfferingTime>> timesByDay
    ) {

        int count = 0;

        for (List<OfferingTime> dayTimes : timesByDay.values()) {

            boolean hasLunchClass = dayTimes.stream()
                    .anyMatch(time ->
                            time.getStartTime().isBefore(LUNCH_END)
                                    && time.getEndTime().isAfter(LUNCH_START)
                    );

            if (!hasLunchClass) {
                count++;
            }
        }

        return count;
    }

    /**
     * 15시 이전 종료하는 요일 수
     */
    private int calculateEarlyFinishDayCount(
            Map<DayOfWeek, List<OfferingTime>> timesByDay
    ) {

        int count = 0;

        for (List<OfferingTime> dayTimes : timesByDay.values()) {

            LocalTime latest = dayTimes.stream()
                    .map(OfferingTime::getEndTime)
                    .max(LocalTime::compareTo)
                    .orElse(LocalTime.MAX);

            if (!latest.isAfter(EARLY_FINISH_TIME)) {
                count++;
            }
        }

        return count;
    }
}