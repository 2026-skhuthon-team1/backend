package com.skhuthon_backend.domain.course;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CourseCategoryConverter implements AttributeConverter<CourseCategory, String> {

    @Override
    public String convertToDatabaseColumn(CourseCategory attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.getDbValue();
    }

    @Override
    public CourseCategory convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return CourseCategory.fromDbValue(dbData);
    }
}
