package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminAccessRequest;
import com.sundaychallenge.dto.AdminAccessResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.service.AdminAccessService;
import com.sundaychallenge.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST Controller for managing authorized Admin accounts and access control.
 * Restricted exclusively to users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccessController {

    private static final Logger log = LoggerFactory.getLogger(AdminAccessController.class);

    private final AdminAccessService adminAccessService;
    private final AuthService authService;

    public AdminAccessController(AdminAccessService adminAccessService, AuthService authService) {
        this.adminAccessService = adminAccessService;
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
    public ResponseEntity<List<AdminAccessResponse>> getAllAdmins(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        log.info("[ADMIN CONTROL API] GET /api/admin/admins requested.");
        List<AdminAccessResponse> list = adminAccessService.getAllAdmins();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<AdminAccessResponse> addAdmin(@RequestBody AdminAccessRequest request,
                                                         @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        User currentUser = authService.getAuthenticatedUser(oauth2User);
        log.info("[ADMIN CONTROL API] POST /api/admin/admins requested by email: {}", currentUser.getEmail());
        AdminAccessResponse response = adminAccessService.addAdmin(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeAdmin(@PathVariable Long id,
                                            @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        User currentUser = authService.getAuthenticatedUser(oauth2User);
        log.info("[ADMIN CONTROL API] DELETE /api/admin/admins/{} requested by email: {}", id, currentUser.getEmail());
        adminAccessService.removeAdmin(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
