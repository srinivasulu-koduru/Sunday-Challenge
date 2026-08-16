package com.sundaychallenge.controller;

import com.sundaychallenge.dto.LeaderboardPageResponse;
import com.sundaychallenge.dto.LeaderboardUserPositionResponse;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.service.AuthService;
import com.sundaychallenge.service.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller providing public/student leaderboard rankings,
 * top 3 podium statistics, filtering, search, pagination, and current user rank summary.
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardController.class);

    private final LeaderboardService leaderboardService;
    private final AuthService authService;

    public LeaderboardController(LeaderboardService leaderboardService, AuthService authService) {
        this.leaderboardService = leaderboardService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<LeaderboardPageResponse> getLeaderboard(
            @RequestParam(value = "challengeId", required = false) Long challengeId,
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "participation", required = false) String participation,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        User currentUser = null;
        if (oauth2User != null) {
            try {
                currentUser = authService.getAuthenticatedUser(oauth2User);
            } catch (Exception e) {
                log.debug("[LEADERBOARD] Unauthenticated or user record not found in session.");
            }
        }

        log.info("[LEADERBOARD API] GET /api/leaderboard requested by user: {}, page: {}, size: {}",
                currentUser != null ? currentUser.getEmail() : "ANONYMOUS", page, size);

        LeaderboardPageResponse response = leaderboardService.getLeaderboardPage(
                challengeId, category, difficulty, period, search, participation, page, size, currentUser
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<LeaderboardUserPositionResponse> getCurrentUserPosition(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            User currentUser = authService.getAuthenticatedUser(oauth2User);
            LeaderboardUserPositionResponse response = leaderboardService.getCurrentUserPosition(currentUser);
            if (response == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
