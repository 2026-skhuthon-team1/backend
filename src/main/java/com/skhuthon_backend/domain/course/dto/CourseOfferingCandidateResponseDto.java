package com.skhuthon_backend.domain.course.dto;

import com.skhuthon_backend.domain.course.entity.Course;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import lombok.Builder;

import java.util.List;

@Builder
public record CourseOfferingCandidateResponseDto(
        Long offeringId,
        String courseCode,
        String courseName,
        Integer credits,
        String category,
        String sectionGroup,
        String offeredYear,
        String sectionNo,
        String professor,
        Boolean yearRestricted,
        Boolean majorRestricted,
        String note,
        List<OfferingTimeResponseDto> times
) {

    public static CourseOfferingCandidateResponseDto of(
            CourseOffering courseOffering,
            List<OfferingTimeResponseDto> times
    ) {
        Course course = courseOffering.getCourse();

        return CourseOfferingCandidateResponseDto.builder()
                .offeringId(courseOffering.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .credits(course.getCredits())
                .category(courseOffering.getCategory().getLabel())
                .sectionGroup(courseOffering.getSectionGroup())
                .offeredYear(courseOffering.getOfferedYear())
                .sectionNo(courseOffering.getSectionNo())
                .professor(courseOffering.getProfessor())
                .yearRestricted(courseOffering.getYearRestricted())
                .majorRestricted(courseOffering.getMajorRestricted())
                .note(courseOffering.getNote())
                .times(times)
                .build();
    }
}
