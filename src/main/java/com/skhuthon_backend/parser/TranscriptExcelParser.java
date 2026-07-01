package com.skhuthon_backend.parser;

import com.skhuthon_backend.parser.exception.InvalidExcelFormatException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class TranscriptExcelParser {

    private static final String COURSE_CODE_HEADER = "과목코드";

    public Set<String> parse(MultipartFile file) {

        validateFile(file);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = findHeaderRow(sheet);
            Map<String, Integer> headerIndexMap = createHeaderIndexMap(headerRow);
            Integer courseCodeColumn = headerIndexMap.get(COURSE_CODE_HEADER);
            if (courseCodeColumn == null) {
                throw new InvalidExcelFormatException("'과목코드' 컬럼을 찾을 수 없습니다.");
            }

            return parseCompletedCourseCodes(
                    sheet,
                    headerRow.getRowNum() + 1,
                    courseCodeColumn
            );
        } catch (IOException e) {
            throw new InvalidExcelFormatException("엑셀 파일을 읽을 수 없습니다.");
        }
    }

    private Row findHeaderRow(Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (COURSE_CODE_HEADER.equals(getCellValue(cell))) {
                    return row;
                }
            }
        }
        throw new InvalidExcelFormatException("헤더를 찾을 수 없습니다.");
    }

    private Map<String, Integer> createHeaderIndexMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            headerMap.put(
                    getCellValue(cell),
                    cell.getColumnIndex()
            );
        }
        return headerMap;
    }

    private Set<String> parseCompletedCourseCodes(
            Sheet sheet,
            int startRow,
            int courseCodeColumn
    ) {
        Set<String> completedCourseCodes = new HashSet<>();
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            Cell cell = row.getCell(courseCodeColumn);
            if (cell == null) {
                continue;
            }
            String courseCode = getCellValue(cell);
            if (courseCode.isBlank()) {
                continue;
            }

            completedCourseCodes.add(courseCode);
        }
        return completedCourseCodes;
    }

    private String getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING ->
                    cell.getStringCellValue().trim();
            case NUMERIC ->
                    String.valueOf((int) cell.getNumericCellValue());
            case FORMULA ->
                    cell.getCellFormula();
            case BOOLEAN ->
                    String.valueOf(cell.getBooleanCellValue());
            default ->
                    "";
        };
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidExcelFormatException("업로드된 파일이 없습니다.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null ||
                !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new InvalidExcelFormatException("엑셀 파일만 업로드할 수 있습니다.");
        }
    }
}