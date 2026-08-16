package com.sundaychallenge.dto;

import java.util.List;

/**
 * Data Transfer Object for College Student Roster CSV Import responses.
 */
public record RosterImportResponse(
        int totalRows,
        int importedRows,
        int duplicateRows,
        int invalidRows,
        int registeredCount,
        int notRegisteredCount,
        List<String> invalidDetails,
        List<String> messages
) {
    public int totalProcessed() {
        return totalRows;
    }

    public int importedCount() {
        return importedRows;
    }

    public int duplicateCount() {
        return duplicateRows;
    }

    public int errorCount() {
        return invalidRows;
    }
}
