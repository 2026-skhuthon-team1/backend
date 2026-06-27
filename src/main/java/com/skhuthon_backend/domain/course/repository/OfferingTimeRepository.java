package com.skhuthon_backend.domain.course.repository;

import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository for offering time blocks.
public interface OfferingTimeRepository extends JpaRepository<OfferingTime, Long> {

    // Finds all time blocks attached to the given offerings.
    List<OfferingTime> findByCourseOfferingIn(Collection<CourseOffering> courseOfferings);
}
