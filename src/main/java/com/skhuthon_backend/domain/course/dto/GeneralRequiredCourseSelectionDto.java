package com.skhuthon_backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;

public record GeneralRequiredCourseSelectionDto(
        @Schema(description = "교양필수 과목명", example = "말과글")
        @NotBlank(message = "교양필수 과목명은 필수입니다.")
        String courseName,

        @Schema(description = "교수명", example = "오현화")
        @NotBlank(message = "교수명은 필수입니다.")
        String professor,

        @ArraySchema(
                arraySchema = @Schema(description = "강의 요일 목록 (해당 교수의 분반이 하나뿐이면 생략 가능)"),
                schema = @Schema(description = "요일", example = "화")
        )
        @Pattern(regexp = "^[월화수목금토일]$", message = "요일은 월, 화, 수, 목, 금, 토, 일 중 하나여야 합니다.") String day,

        @Schema(description = "시작 시간 (해당 교수의 분반이 하나뿐이면 생략 가능)", example = "09:00:00")
        LocalTime start,

        @Schema(description = "종료 시간 (해당 교수의 분반이 하나뿐이면 생략 가능)", example = "10:50:00")
        LocalTime end
) {
}
