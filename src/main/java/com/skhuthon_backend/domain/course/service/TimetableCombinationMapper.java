package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseCategory;
import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.dto.OfferingTimeResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

//- DTO 변환
//- 총학점, 전공학점, 교양학점, 등교일수 계산

@Component
public class TimetableCombinationMapper {

    private static final CourseCategory MAJOR_CATEGORY = CourseCategory.MAJOR;
    private static final CourseCategory GENERAL_CATEGORY = CourseCategory.GENERAL;

    public List<CourseOfferingCandidateResponseDto> toCourseOfferingResponses(CandidateContext candidateContext) {
        return toCourseOfferingResponses(candidateContext.offerings(), candidateContext.timesByOfferingId());
    }

    public List<TimetableCombinationResponseDto> toTimetableResponses(
            List<TimetableCombination> combinations,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        return IntStream.range(0, combinations.size())
                .mapToObj(index -> toTimetableResponse(
                        index + 1L,
                        combinations.get(index),
                        timesByOfferingId,
                        request
                ))
                .collect(Collectors.toList());
    }

    private TimetableCombinationResponseDto toTimetableResponse(
            Long timetableId,
            TimetableCombination combination,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        int majorCredits = calculateCreditsByCategory(combination.offerings(), MAJOR_CATEGORY);
        int generalCredits = calculateCreditsByCategory(combination.offerings(), GENERAL_CATEGORY);

        return TimetableCombinationResponseDto.builder()
                .timetableId(timetableId)
                .totalCredits(majorCredits + generalCredits)
                .majorCredits(majorCredits)
                .generalCredits(generalCredits)
                .attendanceDays(calculateAttendanceDays(combination.times()))
                .freeDays(request.freeDays() == null ? Collections.emptyList() : request.freeDays())
                .excludeFirstPeriod(request.excludeFirstPeriod())
                .offerings(toCourseOfferingResponses(combination.offerings(), timesByOfferingId))
                .build();
    }

    private List<CourseOfferingCandidateResponseDto> toCourseOfferingResponses(
            List<CourseOffering> courseOfferings,
            Map<Long, List<OfferingTime>> timesByOfferingId
    ) {
        return courseOfferings.stream()
                .map(courseOffering -> CourseOfferingCandidateResponseDto.of(
                        courseOffering,
                        toTimeResponses(timesByOfferingId.getOrDefault(courseOffering.getId(), Collections.emptyList()))
                ))
                .collect(Collectors.toList());
    }

    private List<OfferingTimeResponseDto> toTimeResponses(List<OfferingTime> offeringTimes) {
        return offeringTimes.stream()
                .map(OfferingTimeResponseDto::from)
                .collect(Collectors.toList());
    }

    private int calculateCreditsByCategory(List<CourseOffering> courseOfferings, CourseCategory category) {
        return courseOfferings.stream()
                .filter(courseOffering -> courseOffering.getCategory() == category)
                .mapToInt(this::getCredits)
                .sum();
    }

    private int calculateAttendanceDays(List<OfferingTime> offeringTimes) {
        return (int) offeringTimes.stream()
                .map(OfferingTime::getDayOfWeek)
                .distinct()
                .count();
    }

    private int getCredits(CourseOffering courseOffering) {
        Integer credits = courseOffering.getCourse().getCredits();
        if (credits == null) {
            return 0;
        }

        return credits;
    }
}
