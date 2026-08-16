package com.sundaychallenge.dto;

import java.time.LocalDateTime;

public record NotRegisteredStudentResponse(
        Long id,
        String rollNumber,
        String name,
        String email,
        String registrationStatus,
        LocalDateTime addedToRosterAt
) {}
