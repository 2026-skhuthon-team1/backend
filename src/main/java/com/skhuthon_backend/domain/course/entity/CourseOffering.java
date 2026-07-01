package com.skhuthon_backend.domain.course.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@Table(name = "course_offering")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_code", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 4)
    private CourseCategory category;

    @Column(name = "section_group", nullable = false, length = 60)
    private String sectionGroup;

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

    @Builder.Default
    @OneToMany(mappedBy = "courseOffering", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferingTime> offeringTimes = new ArrayList<>();
}
