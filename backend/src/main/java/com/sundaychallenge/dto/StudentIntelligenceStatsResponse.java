package com.sundaychallenge.dto;

public record StudentIntelligenceStatsResponse(
        long totalCollegeRoster,
        long registeredStudents,
        long notRegisteredStudents,
        long studentsParticipated,
        long studentsNeverParticipated,
        long activeStudents,
        long totalAttempts,
        double averageScore,
        double completionRate
) {}
