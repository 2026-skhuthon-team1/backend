package com.skhuthon_backend.domain.course.repository;

import com.skhuthon_backend.domain.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;

// 과목 기본 정보를 조회/저장하는 Repository
public interface CourseRepository extends JpaRepository<Course, String> {
}
