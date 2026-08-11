package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminStatsResponse;
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

/**
 * Controller for admin platform statistics.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
    }

    private User verifyAdmin(OAuth2User oauth2User) {
        try {
            User user = authService.getAuthenticatedUser(oauth2User);
            if (user.getRole() != Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Admin authorization required");
            }
            return user;
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getAdminStats(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getAdminStats());
    }
}
