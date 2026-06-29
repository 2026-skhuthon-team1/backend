package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseCategory;
import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//- DFS / 백트래킹
//- 학점 합 검사
//- 시간 충돌 검사
//- 중복 과목코드 방지

@Component
@RequiredArgsConstructor
public class TimetableCombinationGenerator {

    private static final CourseCategory MAJOR_CATEGORY = CourseCategory.MAJOR;
    private static final CourseCategory GENERAL_CATEGORY = CourseCategory.GENERAL;
    private static final int MAX_COMBINATION_COUNT = 100;

    private final TimeConflictChecker timeConflictChecker;

    public List<TimetableCombination> generate(
            List<CourseOffering> candidates,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        List<TimetableCombination> results = new ArrayList<>();
        int[] remainingMajorCredits = calculateRemainingCreditsByCategory(candidates, MAJOR_CATEGORY);
        int[] remainingGeneralCredits = calculateRemainingCreditsByCategory(candidates, GENERAL_CATEGORY);

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
                results
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
            Set<String> selectedCourseCodes,
            int majorCredits,
            int generalCredits,
            List<TimetableCombination> results
    ) {
        if (results.size() >= MAX_COMBINATION_COUNT) {
            return;
        }

        if (majorCredits == request.getTargetMajorCredits()
                && generalCredits == request.getTargetGeneralCredits()) {
            results.add(new TimetableCombination(
                    List.copyOf(selectedOfferings),
                    List.copyOf(selectedTimes)
            ));
            return;
        }

        if (index >= candidates.size()
                || majorCredits > request.getTargetMajorCredits()
                || generalCredits > request.getTargetGeneralCredits()
                || majorCredits + remainingMajorCredits[index] < request.getTargetMajorCredits()
                || generalCredits + remainingGeneralCredits[index] < request.getTargetGeneralCredits()) {
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
                results
        );

        if (selectedCourseCodes.contains(courseCode)) {
            return;
        }

        int nextMajorCredits = majorCredits;
        int nextGeneralCredits = generalCredits;

        if (candidate.getCategory() == MAJOR_CATEGORY) {
            nextMajorCredits += credits;
        } else if (candidate.getCategory() == GENERAL_CATEGORY) {
            nextGeneralCredits += credits;
        }

        if (nextMajorCredits > request.getTargetMajorCredits()
                || nextGeneralCredits > request.getTargetGeneralCredits()) {
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
                results
        );

        selectedCourseCodes.remove(courseCode);
        selectedTimes.subList(selectedTimes.size() - candidateTimes.size(), selectedTimes.size()).clear();
        selectedOfferings.remove(selectedOfferings.size() - 1);
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
}
