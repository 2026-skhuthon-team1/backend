package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseCategory;
import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.repository.CourseOfferingRepository;
import com.skhuthon_backend.domain.course.repository.OfferingTimeRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//- 전공/교양 후보 조회
//- 학년 조건 적용
//- 기이수 과목 제외
//- OfferingTime 조회

@Component
@RequiredArgsConstructor
public class CourseCandidateProvider {

    private static final CourseCategory MAJOR_CATEGORY = CourseCategory.MAJOR;
    private static final CourseCategory GENERAL_CATEGORY = CourseCategory.GENERAL;
    private static final List<String> SELECTABLE_MAJOR_COURSE_TYPES = List.of("전필", "전선");

    private final CourseOfferingRepository courseOfferingRepository;
    private final OfferingTimeRepository offeringTimeRepository;

    public CandidateContext findCandidates(CourseCandidateRequestDto request) {
        return findCandidates(
                request.getStudentMajors(),
                request.getStudentYear(),
                request.getCompletedCourseCodes()
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
                .filter(courseOffering -> SELECTABLE_MAJOR_COURSE_TYPES.contains(courseOffering.getCourseType()))
                .collect(Collectors.toList());
        List<CourseOffering> generalOfferings = courseOfferingRepository.findByCategory(GENERAL_CATEGORY);
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

        return courseOfferingRepository.findByCategoryAndSectionGroupIn(MAJOR_CATEGORY, studentMajors);
    }

    private List<CourseOffering> findGeneralOfferings(Integer studentYear) {
        return courseOfferingRepository.findByCategory(GENERAL_CATEGORY).stream()
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
