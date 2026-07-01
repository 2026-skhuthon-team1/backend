package com.skhuthon_backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseCandidateRequestDto {

    @Schema(description = "학생의 본전공 및 복수전공 목록", example = "[\"소프트웨어융합전공\", \"인공지능전공\"]")
    @NotEmpty(message = "전공 목록은 필수입니다.")
    @Size(max = 2, message = "전공은 최대 2개까지 입력할 수 있습니다.")
    private List<@NotBlank(message = "전공명은 비어 있을 수 없습니다.") String> studentMajors;

    @Schema(description = "학생의 현재 학년", example = "3")
    @NotNull(message = "학년은 필수입니다.")
    @Min(value = 2, message = "학년은 2 이상이어야 합니다.")
    @Max(value = 4, message = "학년은 4 이하여야 합니다.")
    private Integer studentYear;

    @Schema(description = "이수 완료하여 후보에서 제외할 과목 코드 목록", example = "[\"IS00001\", \"BI00001\"]")
    @NotNull(message = "기이수 과목 코드 목록은 필수입니다.")
    private List<
            @NotBlank(message = "과목 코드는 비어 있을 수 없습니다.")
            @Pattern(regexp = "^[A-Z]{2}\\d{5}$", message = "과목 코드는 영문 대문자 2자리와 숫자 5자리 형식이어야 합니다.")
            String> completedCourseCodes;

    @Builder
    public CourseCandidateRequestDto(
            List<String> studentMajors,
            Integer studentYear,
            List<String> completedCourseCodes
    ) {
        this.studentMajors = studentMajors;
        this.studentYear = studentYear;
        this.completedCourseCodes = completedCourseCodes;
    }
}
