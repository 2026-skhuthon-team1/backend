package com.skhuthon_backend.domain.course.dto;

import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record GeneralRequiredCourseSelectionDto(
        @Schema(description = "교양필수 과목명", example = "말과글")
        @NotBlank(message = "교양필수 과목명은 필수입니다.")
        String courseName,

        @Schema(description = "교수명", example = "오현화")
        @NotBlank(message = "교수명은 필수입니다.")
        String professor,

        @Schema(description = "요일", example = "TUE")
        @NotNull(message = "요일은 필수입니다.")
        DayOfWeek dayOfWeek,

        @Schema(description = "시작 시간", example = "09:00:00")
        @NotNull(message = "시작 시간은 필수입니다.")
        LocalTime startTime,

        @Schema(description = "종료 시간", example = "10:50:00")
        @NotNull(message = "종료 시간은 필수입니다.")
        LocalTime endTime
) {
}
