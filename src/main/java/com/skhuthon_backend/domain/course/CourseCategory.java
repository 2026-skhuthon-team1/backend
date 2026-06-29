package com.skhuthon_backend.domain.course;

import java.util.Arrays;

public enum CourseCategory {

    GENERAL("교양"),
    MAJOR("전공");

    private final String dbValue;

    CourseCategory(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CourseCategory fromDbValue(String dbValue) {
        String normalizedDbValue = dbValue.trim();

        return Arrays.stream(values())
                .filter(category -> category.dbValue.equals(normalizedDbValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown course category: " + dbValue));
    }
}
