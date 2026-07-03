package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableFeature;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//- DFS / 백트래킹
//- 학점 합 검사
//- 시간 충돌 검사
//- 중복 과목코드 방지

@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableCombinationGenerator {

    private static final int MAX_COMBINATION_COUNT = 100;

    private final TimeConflictChecker timeConflictChecker;
    private final TimetableFeatureExtractor timetableFeatureExtractor;

    public List<TimetableCombination> generate(
            List<CourseOffering> candidates,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        List<TimetableCombination> results = new ArrayList<>();
        Set<String> signatures = new HashSet<>();
        int[] remainingElectiveCredits = calculateRemainingCreditsByCategory(candidates, CourseCategory.MAJOR_ELECTIVE);
        int[] remainingRequiredCredits = calculateRemainingCreditsByCategory(candidates, CourseCategory.MAJOR_REQUIRED);
        int[] remainingMajorCredits = IntStream.range(0, remainingElectiveCredits.length)
                .map(index -> remainingElectiveCredits[index] + remainingRequiredCredits[index])
                .toArray();
        int[] remainingGeneralCredits = calculateRemainingCreditsByCategory(candidates, CourseCategory.GENERAL);

        if (log.isDebugEnabled()) {
            candidates.forEach(candidate -> log.debug(
                    "후보 강의: code={}, name={}, category={}, sectionGroup={}, offeredYear={}, credits={}, times=[{}]",
                    candidate.getCourse().getCourseCode(),
                    candidate.getCourse().getCourseName(),
                    candidate.getCategory(),
                    candidate.getSectionGroup(),
                    candidate.getOfferedYear(),
                    getCredits(candidate),
                    summarizeTimes(timesByOfferingId.getOrDefault(candidate.getId(), Collections.emptyList()))
            ));
        }

        if (remainingMajorCredits.length > 0 && remainingMajorCredits[0] < request.targetMajorCredits()) {
            log.warn("후보 전공학점 합({})이 목표 전공학점({})보다 부족해 조합을 만들 수 없음",
                    remainingMajorCredits[0], request.targetMajorCredits());
        }
        if (remainingGeneralCredits.length > 0 && remainingGeneralCredits[0] < request.targetGeneralCredits()) {
            log.warn("후보 교양학점 합({})이 목표 교양학점({})보다 부족해 조합을 만들 수 없음",
                    remainingGeneralCredits[0], request.targetGeneralCredits());
        }

        backtrack(
                candidates,
                timesByOfferingId,
                request,
                remainingMajorCredits,
                remainingGeneralCredits,
                0,
                new ArrayList<>(),
                new ArrayList<>(),
                new HashSet<>(),
                0,
                0,
                results,
                signatures
        );

        if (results.isEmpty()
                && remainingMajorCredits.length > 0 && remainingMajorCredits[0] >= request.targetMajorCredits()
                && remainingGeneralCredits.length > 0 && remainingGeneralCredits[0] >= request.targetGeneralCredits()) {
            log.warn("학점 합은 충분하지만(전공={}, 교양={}) 시간 충돌 또는 공강요일({}) 제약으로 조합을 만들지 못함",
                    remainingMajorCredits[0], remainingGeneralCredits[0], request.freeDays());
        }

        return results;
    }

    private void backtrack(
            List<CourseOffering> candidates,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request,
            int[] remainingMajorCredits,
            int[] remainingGeneralCredits,
            int index,
            List<CourseOffering> selectedOfferings,
            List<OfferingTime> selectedTimes,
            Set<String> selectedCourseCodes,
            int majorCredits,
            int generalCredits,
            List<TimetableCombination> results,
            Set<String> signatures
    ) {
        if (signatures.size() >= MAX_COMBINATION_COUNT) {
            return;
        }

        if (majorCredits == request.targetMajorCredits()
                && generalCredits == request.targetGeneralCredits()) {

            if (!hasRequiredFreeDays(selectedTimes, request.freeDays())) {
                return;
            }

            TimetableCombination combination = new TimetableCombination(
                    List.copyOf(selectedOfferings),
                    List.copyOf(selectedTimes)
            );

            TimetableFeature feature = timetableFeatureExtractor.extract(combination);
            if (signatures.add(feature.signature())) {
                results.add(combination);
            }
            return;
        }

        if (index >= candidates.size()
                || majorCredits > request.targetMajorCredits()
                || generalCredits > request.targetGeneralCredits()
                || majorCredits + remainingMajorCredits[index] < request.targetMajorCredits()
                || generalCredits + remainingGeneralCredits[index] < request.targetGeneralCredits()) {
            return;
        }

        CourseOffering candidate = candidates.get(index);
        String courseCode = candidate.getCourse().getCourseCode();
        int credits = getCredits(candidate);

        backtrack(
                candidates,
                timesByOfferingId,
                request,
                remainingMajorCredits,
                remainingGeneralCredits,
                index + 1,
                selectedOfferings,
                selectedTimes,
                selectedCourseCodes,
                majorCredits,
                generalCredits,
                results,
                signatures
        );

        if (selectedCourseCodes.contains(courseCode)) {
            return;
        }

        int nextMajorCredits = majorCredits;
        int nextGeneralCredits = generalCredits;

        if (candidate.getCategory() == CourseCategory.MAJOR_ELECTIVE || candidate.getCategory() == CourseCategory.MAJOR_REQUIRED) {
            nextMajorCredits += credits;
        } else if (candidate.getCategory() == CourseCategory.GENERAL) {
            nextGeneralCredits += credits;
        }

        if (nextMajorCredits > request.targetMajorCredits()
                || nextGeneralCredits > request.targetGeneralCredits()) {
            return;
        }

        List<OfferingTime> candidateTimes = timesByOfferingId.getOrDefault(candidate.getId(), Collections.emptyList());
        if (timeConflictChecker.hasConflict(selectedTimes, candidateTimes)) {
            return;
        }

        selectedOfferings.add(candidate);
        selectedTimes.addAll(candidateTimes);
        selectedCourseCodes.add(courseCode);

        backtrack(
                candidates,
                timesByOfferingId,
                request,
                remainingMajorCredits,
                remainingGeneralCredits,
                index + 1,
                selectedOfferings,
                selectedTimes,
                selectedCourseCodes,
                nextMajorCredits,
                nextGeneralCredits,
                results,
                signatures
        );

        selectedCourseCodes.remove(courseCode);
        selectedTimes.subList(selectedTimes.size() - candidateTimes.size(), selectedTimes.size()).clear();
        selectedOfferings.remove(selectedOfferings.size() - 1);
    }

    private String summarizeTimes(List<OfferingTime> times) {
        return times.stream()
                .map(time -> time.getDayOfWeek().getLabel() + " " + time.getStartTime() + "-" + time.getEndTime())
                .collect(Collectors.joining(", "));
    }

    private int[] calculateRemainingCreditsByCategory(List<CourseOffering> candidates, CourseCategory category) {
        int[] remainingCredits = new int[candidates.size() + 1];

        for (int index = candidates.size() - 1; index >= 0; index--) {
            CourseOffering candidate = candidates.get(index);
            int additionalCredits = candidate.getCategory() == category ? getCredits(candidate) : 0;
            remainingCredits[index] = remainingCredits[index + 1] + additionalCredits;
        }

        return remainingCredits;
    }

    private int getCredits(CourseOffering courseOffering) {
        Integer credits = courseOffering.getCourse().getCredits();
        if (credits == null) {
            return 0;
        }

        return credits;
    }

    private boolean hasRequiredFreeDays(
            List<OfferingTime> selectedTimes,
            List<String> requiredFreeDays
    ) {

        if (requiredFreeDays == null || requiredFreeDays.isEmpty()) {
            return true;
        }

        Set<String> attendanceDays = selectedTimes.stream()
                .map(OfferingTime::getDayOfWeek)
                .map(DayOfWeek::getLabel)
                .collect(Collectors.toSet());

        return requiredFreeDays.stream()
                .noneMatch(attendanceDays::contains);
    }
}
