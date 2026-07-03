package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.dto.OfferingTimeResponseDto;
import com.skhuthon_backend.domain.course.dto.RecommendedCourseDto;
import com.skhuthon_backend.domain.course.dto.RecommendedCourseTimeDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableRecommendationResponseDto;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//- DTO 변환
//- 총학점, 전공학점, 교양학점, 등교일수 계산

@Component
public class TimetableCombinationMapper {

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
        int majorCredits = calculateCreditsByCategory(combination.offerings(), CourseCategory.MAJOR_ELECTIVE) + calculateCreditsByCategory(combination.offerings(), CourseCategory.MAJOR_REQUIRED) ;
        int generalCredits = calculateCreditsByCategory(combination.offerings(), CourseCategory.GENERAL);

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

    public List<TimetableRecommendationResponseDto> toRecommendationResponses(
            List<AiRankingResponseDto> rankings,
            List<TimetableCombination> combinations
    ) {
        return rankings.stream()
                .map(ranking -> toRecommendationResponse(ranking, combinations))
                .collect(Collectors.toList());
    }

    private TimetableRecommendationResponseDto toRecommendationResponse(
            AiRankingResponseDto ranking,
            List<TimetableCombination> combinations
    ) {
        TimetableCombination combination = combinations.get(Math.toIntExact(ranking.timetableId() - 1));

        return TimetableRecommendationResponseDto.builder()
                .timetableId(ranking.timetableId())
                .score(ranking.score())
                .rank(ranking.rank())
                .tags(ranking.tags())
                .courses(toRecommendedCourses(combination))
                .build();
    }

    private List<RecommendedCourseDto> toRecommendedCourses(TimetableCombination combination) {
        return combination.offerings().stream()
                .map(courseOffering -> toRecommendedCourse(courseOffering, combination.times()))
                .collect(Collectors.toList());
    }

    private RecommendedCourseDto toRecommendedCourse(CourseOffering courseOffering, List<OfferingTime> times) {
        List<OfferingTime> offeringTimes = filterTimesForOffering(courseOffering, times);

        return RecommendedCourseDto.builder()
                .courseName(courseOffering.getCourse().getCourseName())
                .room(findRoom(offeringTimes))
                .category(courseOffering.getCategory().getLabel())
                .professor(courseOffering.getProfessor())
                .credits(getCredits(courseOffering))
                .times(toRecommendedCourseTimes(offeringTimes))
                .build();
    }

    private List<OfferingTime> filterTimesForOffering(CourseOffering courseOffering, List<OfferingTime> times) {
        return times.stream()
                .filter(time -> time.getCourseOffering().getId().equals(courseOffering.getId()))
                .collect(Collectors.toList());
    }

    private String findRoom(List<OfferingTime> offeringTimes) {
        return offeringTimes.stream()
                .map(OfferingTime::getRoom)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<RecommendedCourseTimeDto> toRecommendedCourseTimes(List<OfferingTime> offeringTimes) {
        return offeringTimes.stream()
                .map(RecommendedCourseTimeDto::from)
                .collect(Collectors.toList());
    }

    private int getCredits(CourseOffering courseOffering) {
        Integer credits = courseOffering.getCourse().getCredits();
        if (credits == null) {
            return 0;
        }

        return credits;
    }
}
