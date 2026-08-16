package com.sundaychallenge.dto;

import java.time.LocalDateTime;

public record StudentRosterResponse(
        Long id,
        String rollNumber,
        String name,
        String email,
        boolean isRegisteredOnPortal,
        LocalDateTime createdAt
) {}
