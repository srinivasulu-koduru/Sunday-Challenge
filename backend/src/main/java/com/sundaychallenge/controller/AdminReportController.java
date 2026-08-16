package com.sundaychallenge.controller;

import com.sundaychallenge.dto.CategoryReportResponse;
import com.sundaychallenge.dto.ChallengeReportResponse;
import com.sundaychallenge.dto.DifficultyReportResponse;
import com.sundaychallenge.dto.LeaderboardEntryResponse;
import com.sundaychallenge.dto.LeaderboardPageResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.service.AdminService;
import com.sundaychallenge.service.AuthService;
import com.sundaychallenge.service.LeaderboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller for admin leaderboard and analytical reporting.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminReportController {

    private final AdminService adminService;
    private final AuthService authService;
    private final LeaderboardService leaderboardService;

    public AdminReportController(AdminService adminService, AuthService authService, LeaderboardService leaderboardService) {
        this.adminService = adminService;
        this.authService = authService;
        this.leaderboardService = leaderboardService;
    }

    private void verifyAdmin(OAuth2User oauth2User) {
        try {
            User user = authService.getAuthenticatedUser(oauth2User);
            if (user.getRole() != Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Admin authorization required");
            }
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<LeaderboardPageResponse> getLeaderboardPage(
            @RequestParam(value = "challengeId", required = false) Long challengeId,
            @RequestParam(value = "category", required = false) Category category,
            @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "participation", required = false) String participation,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        User currentUser = authService.getAuthenticatedUser(oauth2User);
        return ResponseEntity.ok(leaderboardService.getLeaderboardPage(
                challengeId, category, difficulty, period, search, participation, page, size, currentUser
        ));
    }

    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getLeaderboard());
    }

    @GetMapping("/reports/challenges")
    public ResponseEntity<List<ChallengeReportResponse>> getChallengeReports(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getChallengeReports());
    }

    @GetMapping("/reports/categories")
    public ResponseEntity<List<CategoryReportResponse>> getCategoryReports(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getCategoryReports());
    }

    @GetMapping("/reports/difficulties")
    public ResponseEntity<List<DifficultyReportResponse>> getDifficultyReports(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getDifficultyReports());
    }
}
