package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import java.util.List;

public record TimetableCombination(
        List<CourseOffering> offerings,
        List<OfferingTime> times
) {
}
