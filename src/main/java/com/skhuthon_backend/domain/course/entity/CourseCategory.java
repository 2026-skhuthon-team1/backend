package com.skhuthon_backend.domain.course.entity;

import java.util.Arrays;

public enum CourseCategory {

    MAJOR_REQUIRED("전공필수"),
    MAJOR_ELECTIVE("전공선택"),
    GENERAL("교양");

    private final String label;

    CourseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static CourseCategory fromLabel(String label) {
        String normalizedDbValue = label.trim();

        return Arrays.stream(values())
                .filter(category -> category.label.equals(normalizedDbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown course category: " + label));
    }
}
