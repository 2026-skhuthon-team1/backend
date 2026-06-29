package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseCategory;
import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.dto.OfferingTimeResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableEngineService {

    private final CourseCandidateProvider courseCandidateProvider;
    private final TimetableConstraintFilter timetableConstraintFilter;
    private final TimetableCombinationGenerator timetableCombinationGenerator;
    private final TimetableCombinationMapper timetableCombinationMapper;
    private static final CourseCategory MAJOR_CATEGORY = CourseCategory.MAJOR;
    private static final CourseCategory GENERAL_CATEGORY = CourseCategory.GENERAL;
    private static final List<String> SELECTABLE_MAJOR_COURSE_TYPES = List.of("전필", "전선");

    private final CourseOfferingRepository courseOfferingRepository;
    private final OfferingTimeRepository offeringTimeRepository;

    @Transactional(readOnly = true)
    public List<TimetableCombinationResponseDto> generateCombinations(TimetableCombinationRequestDto request) {
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request);
        List<CourseOffering> filteredOfferings = timetableConstraintFilter.apply(
                candidateContext.offerings(),
                candidateContext.timesByOfferingId(),
                request
        );
        List<TimetableCombination> combinations = timetableCombinationGenerator.generate(
                filteredOfferings,
                candidateContext.timesByOfferingId(),
                request
        );

        return timetableCombinationMapper.toTimetableResponses(
                combinations,
                candidateContext.timesByOfferingId(),
                request
        );
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingCandidateResponseDto> findAllOfferings() {
        List<CourseOffering> courseOfferings = courseOfferingRepository.findAll();
        Map<Long, List<OfferingTime>> timesByOfferingId = findTimesByOfferingId(courseOfferings);

        return toCourseOfferingResponses(courseOfferings, timesByOfferingId);
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingCandidateResponseDto> findSelectableOfferings(List<String> studentMajors) {
        List<CourseOffering> selectableMajorOfferings = findMajorOfferings(studentMajors).stream()
                .filter(courseOffering -> SELECTABLE_MAJOR_COURSE_TYPES.contains(courseOffering.getCourseType()))
                .collect(Collectors.toList());
        List<CourseOffering> generalOfferings = courseOfferingRepository.findByCategory(GENERAL_CATEGORY);
        List<CourseOffering> selectableOfferings = mergeWithoutDuplicate(selectableMajorOfferings, generalOfferings);
        Map<Long, List<OfferingTime>> timesByOfferingId = findTimesByOfferingId(selectableOfferings);

        return toCourseOfferingResponses(selectableOfferings, timesByOfferingId);
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingCandidateResponseDto> findCandidateOfferings(CourseCandidateRequestDto request) {
        List<CourseOffering> majorOfferings = findMajorOfferings(request.getStudentMajors());
        List<CourseOffering> generalOfferings = findGeneralOfferings(request.getStudentYear());

        List<CourseOffering> candidateOfferings = mergeWithoutDuplicate(majorOfferings, generalOfferings);
        Set<String> completedCodeSet = toSet(request.getCompletedCourseCodes());

        List<CourseOffering> filteredOfferings = candidateOfferings.stream()
                .filter(courseOffering -> !completedCodeSet.contains(courseOffering.getCourse().getCourseCode()))
                .collect(Collectors.toList());

        Map<Long, List<OfferingTime>> timesByOfferingId = findTimesByOfferingId(filteredOfferings);

        return toCourseOfferingResponses(filteredOfferings, timesByOfferingId);
    }

    private List<CourseOffering> findMajorOfferings(List<String> studentMajors) {
        if (studentMajors == null || studentMajors.isEmpty()) {
            return Collections.emptyList();
        }

        return courseOfferingRepository.findByCategoryAndSectionGroupIn(MAJOR_CATEGORY, studentMajors);
    }
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request);

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

    private List<OfferingTimeResponseDto> toTimeResponses(List<OfferingTime> offeringTimes) {
        return offeringTimes.stream()
                .map(OfferingTimeResponseDto::from)
                .collect(Collectors.toList());
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

    private boolean isTimeConflict(OfferingTime first, OfferingTime second) {
        if (!first.getDayOfWeek().equals(second.getDayOfWeek())) {
            return false;
        }

        return first.getStartTime().isBefore(second.getEndTime())
                && second.getStartTime().isBefore(first.getEndTime());
        return timetableCombinationMapper.toCourseOfferingResponses(candidateContext);
    }
}
