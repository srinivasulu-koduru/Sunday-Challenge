package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminAttemptDetailsResponse;
import com.sundaychallenge.dto.AdminAttemptResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.service.AdminService;
import com.sundaychallenge.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller for admin attempt monitoring and inspection.
 */
@RestController
@RequestMapping("/api/admin/attempts")
public class AdminAttemptController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminAttemptController(AdminService adminService, AuthService authService) {
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

    @GetMapping
    public ResponseEntity<List<AdminAttemptResponse>> getAllAttempts(@RequestParam(value = "studentId", required = false) Long studentId,
                                                                      @RequestParam(value = "username", required = false) String username,
                                                                      @RequestParam(value = "challengeId", required = false) Long challengeId,
                                                                      @RequestParam(value = "category", required = false) Category category,
                                                                      @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
                                                                      @RequestParam(value = "status", required = false) AttemptStatus status,
                                                                      @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getAllAttempts(studentId, username, challengeId, category, difficulty, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminAttemptDetailsResponse> getAttemptDetails(@PathVariable("id") Long id,
                                                                          @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getAttemptDetails(id));
    }
}
