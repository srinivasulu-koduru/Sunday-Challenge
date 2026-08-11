package com.sundaychallenge.controller;

import com.sundaychallenge.dto.CategoryReportResponse;
import com.sundaychallenge.dto.ChallengeReportResponse;
import com.sundaychallenge.dto.DifficultyReportResponse;
import com.sundaychallenge.dto.LeaderboardEntryResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.service.AdminService;
import com.sundaychallenge.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public AdminReportController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
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
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(@AuthenticationPrincipal OAuth2User oauth2User) {
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
