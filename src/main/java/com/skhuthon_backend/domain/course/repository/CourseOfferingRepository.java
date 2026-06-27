package com.skhuthon_backend.domain.course.repository;

import com.skhuthon_backend.domain.course.CourseOffering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repository for course offerings.
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    // Finds offerings by category and section group names.
    List<CourseOffering> findByCategoryAndSectionGroupIn(String category, Collection<String> sectionGroups);

    // Finds a unique offering by course code and section number.
    Optional<CourseOffering> findByCourse_CourseCodeAndSectionNo(String courseCode, String sectionNo);

    // Finds general courses with no year restriction or a matching student year.
    @Query("""
            select courseOffering
            from CourseOffering courseOffering
            where courseOffering.category = :category
              and (
                  courseOffering.yearRestricted = false
                  or courseOffering.yearRestricted is null
                  or courseOffering.offeredYear is null
                  or courseOffering.offeredYear like concat('%', :studentYear, '%')
              )
            """)
    List<CourseOffering> findAvailableGeneralOfferings(
            @Param("category") String category,
            @Param("studentYear") String studentYear
    );
}
