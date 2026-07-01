package com.skhuthon_backend.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class TranscriptParserService {

    private final TranscriptExcelParser parser;

    public Set<String> parse(MultipartFile file) {
        return parser.parse(file);
    }
}