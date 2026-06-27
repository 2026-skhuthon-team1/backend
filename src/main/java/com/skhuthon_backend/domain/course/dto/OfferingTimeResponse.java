package com.skhuthon_backend.domain.course.dto;

import com.skhuthon_backend.domain.course.OfferingTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OfferingTimeResponse {

    private final Long id;
    private final String room;
    private final String dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;

    @Builder
    public OfferingTimeResponse(Long id, String room, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static OfferingTimeResponse from(OfferingTime offeringTime) {
        return OfferingTimeResponse.builder()
                .id(offeringTime.getId())
                .room(offeringTime.getRoom())
                .dayOfWeek(offeringTime.getDayOfWeek())
                .startTime(offeringTime.getStartTime())
                .endTime(offeringTime.getEndTime())
                .build();
    }
}
