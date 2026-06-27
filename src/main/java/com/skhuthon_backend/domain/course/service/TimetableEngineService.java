package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponse;
import com.skhuthon_backend.domain.course.dto.OfferingTimeResponse;
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

    private static final String MAJOR_CATEGORY = "\uC804\uACF5";
    private static final String GENERAL_CATEGORY = "\uAD50\uC591";

    private final CourseOfferingRepository courseOfferingRepository;
    private final OfferingTimeRepository offeringTimeRepository;

    @Transactional(readOnly = true)
    public List<CourseOfferingCandidateResponse> findCandidateOfferings(
            List<String> studentMajors,
            Integer studentYear,
            List<String> completedCourseCodes
    ) {
        List<CourseOffering> majorOfferings = findMajorOfferings(studentMajors);
        List<CourseOffering> generalOfferings = courseOfferingRepository.findAvailableGeneralOfferings(
                GENERAL_CATEGORY,
                String.valueOf(studentYear)
        );

        List<CourseOffering> candidateOfferings = mergeWithoutDuplicate(majorOfferings, generalOfferings);
        Set<String> completedCodeSet = toSet(completedCourseCodes);

        List<CourseOffering> filteredOfferings = candidateOfferings.stream()
                .filter(courseOffering -> !completedCodeSet.contains(courseOffering.getCourse().getCourseCode()))
                .collect(Collectors.toList());

        Map<Long, List<OfferingTime>> timesByOfferingId = findTimesByOfferingId(filteredOfferings);

        return filteredOfferings.stream()
                .map(courseOffering -> CourseOfferingCandidateResponse.of(
                        courseOffering,
                        toTimeResponses(timesByOfferingId.getOrDefault(courseOffering.getId(), Collections.emptyList()))
                ))
                .collect(Collectors.toList());
    }

    private List<CourseOffering> findMajorOfferings(List<String> studentMajors) {
        if (studentMajors == null || studentMajors.isEmpty()) {
            return Collections.emptyList();
        }

        return courseOfferingRepository.findByCategoryAndSectionGroupIn(MAJOR_CATEGORY, studentMajors);
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

    private List<OfferingTimeResponse> toTimeResponses(List<OfferingTime> offeringTimes) {
        return offeringTimes.stream()
                .map(OfferingTimeResponse::from)
                .collect(Collectors.toList());
    }

    private boolean isTimeConflict(OfferingTime first, OfferingTime second) {
        if (!first.getDayOfWeek().equals(second.getDayOfWeek())) {
            return false;
        }

        return first.getStartTime().isBefore(second.getEndTime())
                && second.getStartTime().isBefore(first.getEndTime());
    }
}
