package com.skhuthon_backend.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Course {

    @Id
    @Column(name = "course_code", nullable = false, length = 7)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 120)
    private String courseName;

    @Column(name = "credits")
    private Integer credits;
}
