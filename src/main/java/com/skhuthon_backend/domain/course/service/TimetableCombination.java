package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import java.util.List;

public record TimetableCombination(
        List<CourseOffering> offerings,
        List<OfferingTime> times
) {
}
