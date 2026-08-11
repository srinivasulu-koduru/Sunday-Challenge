package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminStudentDetailsResponse;
import com.sundaychallenge.dto.AdminStudentResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
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
 * Controller for admin student management and profile inspection.
 */
@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminStudentController(AdminService adminService, AuthService authService) {
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
    public ResponseEntity<List<AdminStudentResponse>> getAllStudents(@RequestParam(value = "query", required = false) String query,
                                                                    @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getAllStudents(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminStudentDetailsResponse> getStudentDetails(@PathVariable("id") Long id,
                                                                          @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getStudentDetails(id));
    }
}
