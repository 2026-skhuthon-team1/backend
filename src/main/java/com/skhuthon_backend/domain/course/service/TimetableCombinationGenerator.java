package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableFeature;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimetableCombinationGenerator {

    private static final int MAX_COMBINATION_COUNT = 100;
    private static final Set<String> CHAPEL_COURSE_CODES = Set.of("AK00113", "AK00114");
    private static final String CHAPEL_SELECTION_KEY = "CHAPEL";
    // 교양 목표 학점은 사회봉사를 뺀 교양 학점이다. 사회봉사는 포함 여부로 따로 고정해 넣는다.
    private static final String SOCIAL_SERVICE_KEYWORD = "사회봉사";

    private final TimeConflictChecker timeConflictChecker;
    private final TimetableFeatureExtractor timetableFeatureExtractor;

    public List<TimetableCombination> generate(
            List<CourseOffering> candidates,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        return generate(candidates, timesByOfferingId, request, Collections.emptyList());
    }

    public List<TimetableCombination> generate(
            List<CourseOffering> candidates,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request,
            List<CourseOffering> fixedOfferings
    ) {
        List<CourseOffering> initialOfferings =
                fixedOfferings == null ? Collections.emptyList() : fixedOfferings;
        Set<Long> fixedOfferingIds = initialOfferings.stream()
                .map(CourseOffering::getId)
                .collect(Collectors.toSet());
        List<CourseOffering> selectableCandidates = candidates.stream()
                .filter(candidate -> !fixedOfferingIds.contains(candidate.getId()))
                .collect(Collectors.toList());
        List<OfferingTime> initialTimes = collectTimes(initialOfferings, timesByOfferingId);

        if (hasInternalConflict(initialTimes)) {
            log.warn("선택된 고정 강좌끼리 시간이 충돌하여 시간표를 생성할 수 없음");
            return Collections.emptyList();
        }

        int initialMajorCredits = calculateMajorCredits(initialOfferings);
        int initialGeneralCredits = calculateGeneralCredits(initialOfferings);
        int[] remainingMajorCredits = calculateRemainingMajorCredits(selectableCandidates);
        int[] remainingGeneralCredits = calculateRemainingGeneralCredits(selectableCandidates);

        if (remainingMajorCredits.length > 0
                && initialMajorCredits + remainingMajorCredits[0] < request.targetMajorCredits()) {
            log.warn(
                    "후보 전공학점 합({})이 목표 전공학점({})보다 부족해 조합을 만들 수 없음",
                    initialMajorCredits + remainingMajorCredits[0],
                    request.targetMajorCredits()
            );
        }
        if (remainingGeneralCredits.length > 0
                && initialGeneralCredits + remainingGeneralCredits[0] < request.targetGeneralCredits()) {
            log.warn(
                    "후보 교양학점 합({})이 목표 교양학점({})보다 부족해 조합을 만들 수 없음",
                    initialGeneralCredits + remainingGeneralCredits[0],
                    request.targetGeneralCredits()
            );
        }

        List<TimetableCombination> results = new ArrayList<>();
        Set<String> signatures = new HashSet<>();

        backtrack(
                selectableCandidates,
                timesByOfferingId,
                request,
                remainingMajorCredits,
                remainingGeneralCredits,
                0,
                new ArrayList<>(initialOfferings),
                new ArrayList<>(initialTimes),
                initialTimes.size(),
                initialOfferings.stream()
                        .map(this::toSelectionKey)
                        .collect(Collectors.toSet()),
                initialMajorCredits,
                initialGeneralCredits,
                results,
                signatures
        );

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
            int fixedTimeCount,
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

            // fixedCourses(고정 강좌)로 채워진 시간은 공강 요일 희망과 무관하게 항상 유지되어야 하므로,
            // 자유 선택으로 채워진 부분(fixedTimeCount 이후)만 공강 요일 조건을 검사한다.
            if (!hasRequiredFreeDays(selectedTimes.subList(fixedTimeCount, selectedTimes.size()), request.freeDays())) {
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
        String selectionKey = toSelectionKey(candidate);
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
                fixedTimeCount,
                selectedCourseCodes,
                majorCredits,
                generalCredits,
                results,
                signatures
        );

        if (selectedCourseCodes.contains(selectionKey)) {
            return;
        }

        int nextMajorCredits = majorCredits;
        int nextGeneralCredits = generalCredits;

        if (isMajorCategory(candidate.getCategory())) {
            nextMajorCredits += credits;
        } else if (countsTowardGeneralCredits(candidate)) {
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
        selectedCourseCodes.add(selectionKey);

        backtrack(
                candidates,
                timesByOfferingId,
                request,
                remainingMajorCredits,
                remainingGeneralCredits,
                index + 1,
                selectedOfferings,
                selectedTimes,
                fixedTimeCount,
                selectedCourseCodes,
                nextMajorCredits,
                nextGeneralCredits,
                results,
                signatures
        );

        selectedCourseCodes.remove(selectionKey);
        selectedTimes.subList(selectedTimes.size() - candidateTimes.size(), selectedTimes.size()).clear();
        selectedOfferings.remove(selectedOfferings.size() - 1);
    }

    private String toSelectionKey(CourseOffering courseOffering) {
        String courseCode = courseOffering.getCourse().getCourseCode();
        if (CHAPEL_COURSE_CODES.contains(courseCode)) {
            return CHAPEL_SELECTION_KEY;
        }

        return courseCode;
    }

    private List<OfferingTime> collectTimes(
            List<CourseOffering> offerings,
            Map<Long, List<OfferingTime>> timesByOfferingId
    ) {
        return offerings.stream()
                .flatMap(offering -> timesByOfferingId.getOrDefault(
                        offering.getId(),
                        Collections.emptyList()
                ).stream())
                .collect(Collectors.toList());
    }

    private boolean hasInternalConflict(List<OfferingTime> times) {
        for (int i = 0; i < times.size(); i++) {
            for (int j = i + 1; j < times.size(); j++) {
                if (timeConflictChecker.hasConflict(List.of(times.get(i)), List.of(times.get(j)))) {
                    return true;
                }
            }
        }

        return false;
    }

    private int[] calculateRemainingMajorCredits(List<CourseOffering> candidates) {
        int[] remainingCredits = new int[candidates.size() + 1];

        for (int index = candidates.size() - 1; index >= 0; index--) {
            CourseOffering candidate = candidates.get(index);
            int additionalCredits = isMajorCategory(candidate.getCategory()) ? getCredits(candidate) : 0;
            remainingCredits[index] = remainingCredits[index + 1] + additionalCredits;
        }

        return remainingCredits;
    }

    private int[] calculateRemainingGeneralCredits(List<CourseOffering> candidates) {
        int[] remainingCredits = new int[candidates.size() + 1];

        for (int index = candidates.size() - 1; index >= 0; index--) {
            CourseOffering candidate = candidates.get(index);
            int additionalCredits = countsTowardGeneralCredits(candidate) ? getCredits(candidate) : 0;
            remainingCredits[index] = remainingCredits[index + 1] + additionalCredits;
        }

        return remainingCredits;
    }

    private int calculateMajorCredits(List<CourseOffering> offerings) {
        return offerings.stream()
                .filter(offering -> isMajorCategory(offering.getCategory()))
                .mapToInt(this::getCredits)
                .sum();
    }

    private int calculateGeneralCredits(List<CourseOffering> offerings) {
        return offerings.stream()
                .filter(this::countsTowardGeneralCredits)
                .mapToInt(this::getCredits)
                .sum();
    }

    private boolean isMajorCategory(CourseCategory category) {
        return category == CourseCategory.MAJOR_ELECTIVE
                || category == CourseCategory.MAJOR_REQUIRED
                || category == CourseCategory.MAJOR_EXPLORATION;
    }

    private boolean countsTowardGeneralCredits(CourseOffering offering) {
        return isGeneralCategory(offering.getCategory())
                && !offering.getCourse().getCourseName().contains(SOCIAL_SERVICE_KEYWORD);
    }

    private boolean isGeneralCategory(CourseCategory category) {
        return category == CourseCategory.GENERAL
                || category == CourseCategory.GENERAL_REQUIRED;
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
