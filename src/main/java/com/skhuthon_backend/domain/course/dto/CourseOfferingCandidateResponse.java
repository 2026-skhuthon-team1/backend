package com.skhuthon_backend.domain.course.dto;

import com.skhuthon_backend.domain.course.Course;
import com.skhuthon_backend.domain.course.CourseOffering;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CourseOfferingCandidateResponse {

    private final Long offeringId;
    private final String courseCode;
    private final String courseName;
    private final Integer credits;
    private final String category;
    private final String sectionGroup;
    private final String courseType;
    private final String offeredYear;
    private final String sectionNo;
    private final String professor;
    private final Boolean yearRestricted;
    private final Boolean majorRestricted;
    private final String note;
    private final List<OfferingTimeResponse> times;

    @Builder
    public CourseOfferingCandidateResponse(
            Long offeringId,
            String courseCode,
            String courseName,
            Integer credits,
            String category,
            String sectionGroup,
            String courseType,
            String offeredYear,
            String sectionNo,
            String professor,
            Boolean yearRestricted,
            Boolean majorRestricted,
            String note,
            List<OfferingTimeResponse> times
    ) {
        this.offeringId = offeringId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.category = category;
        this.sectionGroup = sectionGroup;
        this.courseType = courseType;
        this.offeredYear = offeredYear;
        this.sectionNo = sectionNo;
        this.professor = professor;
        this.yearRestricted = yearRestricted;
        this.majorRestricted = majorRestricted;
        this.note = note;
        this.times = times;
    }

    public static CourseOfferingCandidateResponse of(
            CourseOffering courseOffering,
            List<OfferingTimeResponse> times
    ) {
        Course course = courseOffering.getCourse();

        return CourseOfferingCandidateResponse.builder()
                .offeringId(courseOffering.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .credits(course.getCredits())
                .category(courseOffering.getCategory())
                .sectionGroup(courseOffering.getSectionGroup())
                .courseType(courseOffering.getCourseType())
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
