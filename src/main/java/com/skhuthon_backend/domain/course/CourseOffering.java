package com.skhuthon_backend.domain.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_offering")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_code", nullable = false)
    private Course course;

    @Column(name = "category", nullable = false, length = 4)
    private String category;

    @Column(name = "section_group", nullable = false, length = 60)
    private String sectionGroup;

    @Column(name = "course_type", length = 4)
    private String courseType;

    @Column(name = "offered_year", length = 12)
    private String offeredYear;

    @Column(name = "section_no", nullable = false, length = 5)
    private String sectionNo;

    @Column(name = "professor", length = 60)
    private String professor;

    @Column(name = "year_restricted")
    private Boolean yearRestricted;

    @Column(name = "major_restricted")
    private Boolean majorRestricted;

    @Column(name = "note", length = 255)
    private String note;

    @Builder
    public CourseOffering(
            Course course,
            String category,
            String sectionGroup,
            String courseType,
            String offeredYear,
            String sectionNo,
            String professor,
            Boolean yearRestricted,
            Boolean majorRestricted,
            String note
    ) {
        this.course = course;
        this.category = category;
        this.sectionGroup = sectionGroup;
        this.courseType = courseType;
        this.offeredYear = offeredYear;
        this.sectionNo = sectionNo;
        this.professor = professor;
        this.yearRestricted = yearRestricted;
        this.majorRestricted = majorRestricted;
        this.note = note;
    }
}
