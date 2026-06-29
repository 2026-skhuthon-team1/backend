package com.skhuthon_backend.domain.course.dto;

import com.skhuthon_backend.domain.course.OfferingTime;
import java.time.LocalTime;
import lombok.Builder;

@Builder
public record OfferingTimeResponseDto(
        Long id,
        String room,
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {

    public static OfferingTimeResponseDto from(OfferingTime offeringTime) {
        return OfferingTimeResponseDto.builder()
                .id(offeringTime.getId())
                .room(offeringTime.getRoom())
                .dayOfWeek(offeringTime.getDayOfWeek())
                .startTime(offeringTime.getStartTime())
                .endTime(offeringTime.getEndTime())
                .build();
    }
}
