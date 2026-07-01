package com.skhuthon_backend.domain.course.dto;

import com.skhuthon_backend.domain.course.entity.OfferingTime;
import lombok.Builder;

import java.time.LocalTime;

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
                .dayOfWeek(offeringTime.getDayOfWeek().getLabel())
                .startTime(offeringTime.getStartTime())
                .endTime(offeringTime.getEndTime())
                .build();
    }
}
