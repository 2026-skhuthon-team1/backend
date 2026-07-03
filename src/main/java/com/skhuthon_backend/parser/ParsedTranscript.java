package com.skhuthon_backend.parser;

import java.util.Set;

public record ParsedTranscript(
        Set<String> courseCodes,
        Set<String> courseNames
) {
}
