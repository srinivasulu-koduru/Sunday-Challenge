package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminQuestionRequest;
import com.sundaychallenge.dto.AdminQuestionResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.service.AdminService;
import com.sundaychallenge.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller for admin question management.
 */
@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminQuestionController(AdminService adminService, AuthService authService) {
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
    public ResponseEntity<List<AdminQuestionResponse>> getAllQuestions(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getAllQuestions());
    }

    @PostMapping
    public ResponseEntity<AdminQuestionResponse> createQuestion(@RequestBody AdminQuestionRequest request,
                                                               @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createQuestion(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminQuestionResponse> updateQuestion(@PathVariable("id") Long id,
                                                               @RequestBody AdminQuestionRequest request,
                                                               @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.updateQuestion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable("id") Long id,
                                             @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        adminService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
