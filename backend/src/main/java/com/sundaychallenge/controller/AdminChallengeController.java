package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminChallengeDetailsResponse;
import com.sundaychallenge.dto.AdminChallengeRequest;
import com.sundaychallenge.dto.AdminChallengeResponse;
import com.sundaychallenge.dto.AdminQuestionRequest;
import com.sundaychallenge.dto.AdminQuestionResponse;
import com.sundaychallenge.dto.SaveChallengeWithQuestionsRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller for admin challenge management, scheduling, and embedded question bank control.
 */
@RestController
@RequestMapping("/api/admin/challenges")
public class AdminChallengeController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminChallengeController(AdminService adminService, AuthService authService) {
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
    public ResponseEntity<List<AdminChallengeResponse>> getAllChallenges(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getAllChallenges());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminChallengeDetailsResponse> getChallengeDetails(@PathVariable("id") Long id,
                                                                              @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getChallengeDetailsWithQuestions(id));
    }

    @PostMapping
    public ResponseEntity<AdminChallengeResponse> createChallenge(@RequestBody AdminChallengeRequest request,
                                                                 @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createChallenge(request));
    }

    @PostMapping("/full")
    public ResponseEntity<AdminChallengeDetailsResponse> createChallengeWithQuestions(@RequestBody SaveChallengeWithQuestionsRequest request,
                                                                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createChallengeWithQuestions(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminChallengeResponse> updateChallenge(@PathVariable("id") Long id,
                                                                 @RequestBody AdminChallengeRequest request,
                                                                 @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.updateChallenge(id, request));
    }

    @PutMapping("/{id}/full")
    public ResponseEntity<AdminChallengeDetailsResponse> updateChallengeWithQuestions(@PathVariable("id") Long id,
                                                                                        @RequestBody SaveChallengeWithQuestionsRequest request,
                                                                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.updateChallengeWithQuestions(id, request));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<AdminChallengeResponse> toggleChallengeStatusPost(@PathVariable("id") Long id,
                                                                            @RequestParam("active") boolean active,
                                                                            @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.toggleChallengeStatus(id, active));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminChallengeResponse> toggleChallengeStatus(@PathVariable("id") Long id,
                                                                        @RequestParam("active") boolean active,
                                                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.toggleChallengeStatus(id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChallenge(@PathVariable("id") Long id,
                                               @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        adminService.deleteChallenge(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<List<AdminQuestionResponse>> getQuestionsForChallenge(@PathVariable("id") Long id,
                                                                                  @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getQuestionsForChallenge(id));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<AdminQuestionResponse> addQuestionToChallenge(@PathVariable("id") Long id,
                                                                          @RequestBody AdminQuestionRequest request,
                                                                          @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.addQuestionToChallenge(id, request, null));
    }
}
