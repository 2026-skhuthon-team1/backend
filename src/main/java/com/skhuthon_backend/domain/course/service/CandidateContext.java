package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import java.util.List;
import java.util.Map;

public record CandidateContext(
        List<CourseOffering> offerings,
        Map<Long, List<OfferingTime>> timesByOfferingId
) {
}
