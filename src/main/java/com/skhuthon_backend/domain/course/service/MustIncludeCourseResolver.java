package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.exception.MustIncludeCourseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 학생이 담은 "꼭 넣을 과목"을 과목별 분반 묶음으로 만든다. 같은 과목의 분반을 여러 개 담으면 그중 하나만 들어간다.
// 탐색을 돌린 뒤 '조합 없음'만 보여 주면 무엇을 고쳐야 할지 알 수 없으므로,
// 조합이 하나도 나올 수 없는 경우(조건 불만족·반드시 겹침·학점 초과)는 여기서 이유를 담아 바로 알려 준다.
@Component
@RequiredArgsConstructor
public class MustIncludeCourseResolver {

    private final TimetableConstraintFilter timetableConstraintFilter;
    private final TimeConflictChecker timeConflictChecker;
    private final TimetableCombinationGenerator timetableCombinationGenerator;

    /**
     * @param mustIncludeOfferings 학생이 담은 분반들
     * @param fixedOfferings       이미 고정으로 들어가는 분반들(1학년 교양필수 선택). 같은 과목은 꼭 넣을 과목에서 뺀다
     * @return 과목별 분반 묶음. 시간표마다 묶음 하나에서 분반 하나씩 들어간다
     */
    public List<List<CourseOffering>> resolve(
            List<CourseOffering> mustIncludeOfferings,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request,
            List<CourseOffering> fixedOfferings
    ) {
        if (mustIncludeOfferings == null || mustIncludeOfferings.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> fixedCourseCodes = fixedOfferings.stream()
                .map(this::courseCode)
                .collect(Collectors.toSet());
        Map<String, List<CourseOffering>> sectionsByCourseCode = mustIncludeOfferings.stream()
                .filter(offering -> !fixedCourseCodes.contains(courseCode(offering)))
                .collect(Collectors.groupingBy(this::courseCode, LinkedHashMap::new, Collectors.toList()));

        List<List<CourseOffering>> groups = new ArrayList<>();
        for (List<CourseOffering> sections : sectionsByCourseCode.values()) {
            groups.add(selectUsableSections(sections, timesByOfferingId, request));
        }

        validateUnavoidableConflicts(groups, fixedOfferings, timesByOfferingId);
        validateCredits(groups, fixedOfferings, request);

        return groups;
    }

    // 1교시 제외처럼 반드시 지켜야 하는 조건에 맞는 분반만 남긴다. 하나도 안 남으면 담은 과목이 말없이 빠지지 않도록 알려 준다.
    // 공강 요일은 소프트 조건이라, 공강 희망 요일에 없는 분반이 있으면 그 분반만 쓰고 없을 때만 공강 요일 분반도 쓴다.
    private List<CourseOffering> selectUsableSections(
            List<CourseOffering> sections,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        List<CourseOffering> eligible = timetableConstraintFilter.apply(sections, timesByOfferingId, request);
        if (eligible.isEmpty()) {
            throw new MustIncludeCourseException(
                    "'%s'의 분반이 1교시 제외 조건을 만족하지 못합니다. 조건을 풀거나 담은 과목에서 빼 보세요."
                            .formatted(courseName(sections.get(0)))
            );
        }

        List<String> freeDays = request.freeDays() == null ? Collections.emptyList() : request.freeDays();
        List<CourseOffering> notOnFreeDays = eligible.stream()
                .filter(offering -> timesOf(offering, timesByOfferingId).stream()
                        .noneMatch(time -> freeDays.contains(time.getDayOfWeek().getLabel())))
                .collect(Collectors.toList());

        return notOnFreeDays.isEmpty() ? eligible : notOnFreeDays;
    }

    // 어떤 분반을 골라도 반드시 겹치는 과목 쌍을 찾는다. 안 겹치는 분반 조합이 하나라도 있으면 문제가 아니다.
    // (세 과목 이상이 얽혀서 막히는 경우까지는 찾지 않는다. 그때는 결과가 빈 목록으로 나온다)
    private void validateUnavoidableConflicts(
            List<List<CourseOffering>> groups,
            List<CourseOffering> fixedOfferings,
            Map<Long, List<OfferingTime>> timesByOfferingId
    ) {
        List<List<CourseOffering>> checkGroups = new ArrayList<>();
        fixedOfferings.forEach(fixed -> checkGroups.add(List.of(fixed)));
        checkGroups.addAll(groups);

        for (int i = 0; i < checkGroups.size(); i++) {
            for (int j = i + 1; j < checkGroups.size(); j++) {
                if (alwaysConflict(checkGroups.get(i), checkGroups.get(j), timesByOfferingId)) {
                    throw new MustIncludeCourseException(
                            "%s과(와) %s의 시간이 겹칩니다. 둘 중 하나를 빼거나 다른 분반을 담아 주세요."
                                    .formatted(label(checkGroups.get(i)), label(checkGroups.get(j)))
                    );
                }
            }
        }
    }

    private boolean alwaysConflict(
            List<CourseOffering> first,
            List<CourseOffering> second,
            Map<Long, List<OfferingTime>> timesByOfferingId
    ) {
        return first.stream().allMatch(a -> second.stream().allMatch(b ->
                timeConflictChecker.hasConflict(timesOf(a, timesByOfferingId), timesOf(b, timesByOfferingId))));
    }

    // 담은 과목만으로 목표 학점을 넘으면 어떤 조합도 목표 학점을 정확히 맞출 수 없다
    private void validateCredits(
            List<List<CourseOffering>> groups,
            List<CourseOffering> fixedOfferings,
            TimetableCombinationRequestDto request
    ) {
        List<CourseOffering> oneSectionPerCourse = new ArrayList<>(fixedOfferings);
        groups.forEach(group -> oneSectionPerCourse.add(group.get(0)));

        int majorCredits = timetableCombinationGenerator.calculateMajorCredits(oneSectionPerCourse);
        if (majorCredits > request.targetMajorCredits()) {
            throw new MustIncludeCourseException(
                    "담은 과목만으로 전공 %d학점이라 전공 목표 학점(%d)을 넘습니다. 목표 학점을 늘리거나 담은 과목을 빼 보세요."
                            .formatted(majorCredits, request.targetMajorCredits())
            );
        }

        int generalCredits = timetableCombinationGenerator.calculateGeneralCredits(oneSectionPerCourse);
        if (generalCredits > request.targetGeneralCredits()) {
            throw new MustIncludeCourseException(
                    "담은 과목만으로 교양 %d학점이라 교양 목표 학점(%d)을 넘습니다. 목표 학점을 늘리거나 담은 과목을 빼 보세요."
                            .formatted(generalCredits, request.targetGeneralCredits())
            );
        }
    }

    // 분반이 하나면 '몇 분반'까지 적어 어느 분반이 문제인지 알 수 있게 한다
    private String label(List<CourseOffering> sections) {
        CourseOffering first = sections.get(0);
        if (sections.size() == 1) {
            return "%s(%s분반)".formatted(courseName(first), first.getSectionNo());
        }

        return courseName(first);
    }

    private List<OfferingTime> timesOf(CourseOffering offering, Map<Long, List<OfferingTime>> timesByOfferingId) {
        return timesByOfferingId.getOrDefault(offering.getId(), Collections.emptyList());
    }

    private String courseCode(CourseOffering offering) {
        return offering.getCourse().getCourseCode();
    }

    private String courseName(CourseOffering offering) {
        return offering.getCourse().getCourseName();
    }
}
