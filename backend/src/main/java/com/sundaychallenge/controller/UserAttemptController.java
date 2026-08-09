package com.sundaychallenge.controller;

import com.sundaychallenge.dto.UserAttemptSummaryResponse;
import com.sundaychallenge.dto.UserStatsResponse;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.service.AuthService;
import com.sundaychallenge.service.ChallengeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * UserAttemptController provides endpoints for retrieving student attempt history and statistics.
 */
@RestController
@RequestMapping("/api/user")
public class UserAttemptController {

    private static final Logger log = LoggerFactory.getLogger(UserAttemptController.class);

    private final ChallengeService challengeService;
    private final AuthService authService;

    public UserAttemptController(ChallengeService challengeService, AuthService authService) {
        this.challengeService = challengeService;
        this.authService = authService;
    }

    /**
     * GET /api/user/attempts
     * Returns history of attempts for the authenticated student.
     */
    @GetMapping("/attempts")
    public ResponseEntity<List<UserAttemptSummaryResponse>> getUserAttempts(@AuthenticationPrincipal OAuth2User oauth2User) {
        User user = authService.getAuthenticatedUser(oauth2User);
        log.info("[DEBUG] GET /api/user/attempts called by user ID: {}", user.getId());
        return ResponseEntity.ok(challengeService.getUserAttempts(user));
    }

    /**
     * GET /api/user/stats
     * Returns aggregated statistics for the student dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@AuthenticationPrincipal OAuth2User oauth2User) {
        User user = authService.getAuthenticatedUser(oauth2User);
        log.info("[DEBUG] GET /api/user/stats called by user ID: {}", user.getId());
        return ResponseEntity.ok(challengeService.getUserDashboardStats(user));
    }
}
