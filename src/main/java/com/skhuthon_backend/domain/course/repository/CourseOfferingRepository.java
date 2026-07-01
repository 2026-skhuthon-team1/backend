package com.skhuthon_backend.domain.course.repository;

import com.skhuthon_backend.domain.course.entity.Course;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for course offerings.
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    // Finds offerings by category and section group names.
    List<CourseOffering> findByCategoryAndSectionGroupIn(CourseCategory category, Collection<String> sectionGroups);

    // Finds offerings by category.
    List<CourseOffering> findByCategory(CourseCategory category);

    // Finds an offering by course and section number.
    Optional<CourseOffering> findByCourseAndSectionNo(Course course, String sectionNo);

}
