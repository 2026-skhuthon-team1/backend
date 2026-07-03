package com.skhuthon_backend.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TranscriptParserService {

    private final TranscriptExcelParser parser;

    public ParsedTranscript parse(MultipartFile file) {
        return parser.parse(file);
    }
}