package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.repository.CourseOfferingRepository;
import com.skhuthon_backend.domain.course.repository.OfferingTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//- 전공/교양 후보 조회
//- 학년 조건 적용
//- 기이수 과목 제외
//- OfferingTime 조회

@Component
@RequiredArgsConstructor
public class CourseCandidateProvider {

    private static final List<String> SELECTABLE_MAJOR_COURSE_TYPES = List.of("전공필수", "전공선택", "교양");

    // 소프트웨어융합학부(IT융합자율학부) 4학년 트랙 - 2~3학년은 공통 전공으로 개설되므로
    // 트랙을 선언한 학생도 학부 공통 전공 과목을 자격 대상에 포함해야 함
    private static final Set<String> SOFTWARE_CONVERGENCE_TRACKS = Set.of(
            "소프트웨어공학전공", "정보통신공학전공", "컴퓨터공학전공"
    );
    private static final String SOFTWARE_CONVERGENCE_COMMON_MAJOR = "소프트웨어융합전공";

    private final CourseOfferingRepository courseOfferingRepository;
    private final OfferingTimeRepository offeringTimeRepository;

    public CandidateContext findCandidates(CourseCandidateRequestDto request) {
        return findCandidates(
                request.studentMajors(),
                request.studentYear(),
                request.completedCourseCodes()
        );
    }

    public CandidateContext findCandidates(TimetableCombinationRequestDto request) {
        return findCandidates(
                request.studentMajors(),
                request.studentYear(),
                request.completedCourseCodes()
        );
    }

    public CandidateContext findAllOfferings() {
        List<CourseOffering> courseOfferings = courseOfferingRepository.findAll();

        return new CandidateContext(courseOfferings, findTimesByOfferingId(courseOfferings));
    }

    public CandidateContext findSelectableOfferings(List<String> studentMajors) {
        List<CourseOffering> selectableMajorOfferings = findMajorOfferings(studentMajors).stream()
                .filter(courseOffering -> SELECTABLE_MAJOR_COURSE_TYPES.contains(courseOffering.getCategory().getLabel()))
                .collect(Collectors.toList());
        List<CourseOffering> generalOfferings = courseOfferingRepository.findByCategory(CourseCategory.GENERAL);
        List<CourseOffering> selectableOfferings = mergeWithoutDuplicate(selectableMajorOfferings, generalOfferings);

        return new CandidateContext(selectableOfferings, findTimesByOfferingId(selectableOfferings));
    }

    private CandidateContext findCandidates(
            List<String> studentMajors,
            Integer studentYear,
            List<String> completedCourseCodes
    ) {
        List<CourseOffering> majorOfferings = findMajorOfferings(studentMajors);
        List<CourseOffering> generalOfferings = findGeneralOfferings(studentYear);
        List<CourseOffering> candidateOfferings = mergeWithoutDuplicate(majorOfferings, generalOfferings);
        Set<String> completedCodeSet = toSet(completedCourseCodes);

        List<CourseOffering> filteredOfferings = candidateOfferings.stream()
                .filter(courseOffering -> !completedCodeSet.contains(courseOffering.getCourse().getCourseCode()))
                .collect(Collectors.toList());

        return new CandidateContext(filteredOfferings, findTimesByOfferingId(filteredOfferings));
    }

    private List<CourseOffering> findMajorOfferings(List<String> studentMajors) {
        if (studentMajors == null || studentMajors.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> eligibleMajors = resolveEligibleMajors(studentMajors);

        return courseOfferingRepository.findByCategoryInAndSectionGroupIn(List.of(CourseCategory.MAJOR_ELECTIVE, CourseCategory.MAJOR_REQUIRED), eligibleMajors);
    }

    private List<String> resolveEligibleMajors(List<String> studentMajors) {
        Set<String> eligibleMajors = new LinkedHashSet<>(studentMajors);

        if (studentMajors.stream().anyMatch(SOFTWARE_CONVERGENCE_TRACKS::contains)) {
            eligibleMajors.add(SOFTWARE_CONVERGENCE_COMMON_MAJOR);
        }

        return new ArrayList<>(eligibleMajors);
    }

    private List<CourseOffering> findGeneralOfferings(Integer studentYear) {
        return courseOfferingRepository.findByCategory(CourseCategory.GENERAL).stream()
                .filter(courseOffering -> isAvailableForStudentYear(courseOffering, studentYear))
                .collect(Collectors.toList());
    }

    private boolean isAvailableForStudentYear(CourseOffering courseOffering, Integer studentYear) {
        if (courseOffering.getYearRestricted() == null || !courseOffering.getYearRestricted()) {
            return true;
        }

        if (courseOffering.getOfferedYear() == null) {
            return true;
        }

        return courseOffering.getOfferedYear().contains(String.valueOf(studentYear));
    }

    private List<CourseOffering> mergeWithoutDuplicate(
            List<CourseOffering> firstOfferings,
            List<CourseOffering> secondOfferings
    ) {
        Map<Long, CourseOffering> offeringsById = new LinkedHashMap<>();

        firstOfferings.forEach(courseOffering -> offeringsById.put(courseOffering.getId(), courseOffering));
        secondOfferings.forEach(courseOffering -> offeringsById.put(courseOffering.getId(), courseOffering));

        return new ArrayList<>(offeringsById.values());
    }

    private Set<String> toSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        return values.stream().collect(Collectors.toSet());
    }

    private Map<Long, List<OfferingTime>> findTimesByOfferingId(List<CourseOffering> courseOfferings) {
        if (courseOfferings.isEmpty()) {
            return Collections.emptyMap();
        }

        return offeringTimeRepository.findByCourseOfferingIn(courseOfferings).stream()
                .collect(Collectors.groupingBy(offeringTime -> offeringTime.getCourseOffering().getId()));
    }
}
