package com.sundaychallenge.controller;

import com.sundaychallenge.dto.SetUsernameRequest;
import com.sundaychallenge.dto.SetUsernameResponse;
import com.sundaychallenge.dto.UserResponse;
import com.sundaychallenge.dto.UsernameStatusResponse;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.UserRepository;
import com.sundaychallenge.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * UserController provides user profile management and username setup endpoints.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Endpoint to fetch the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(HttpServletRequest request,
                                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        HttpSession session = request.getSession(false);
        String sessionId = (session != null) ? session.getId() : "NO_SESSION";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("[DEBUG] GET /api/user/me request received. Session ID: {}, Authentication: {}, OAuth2User: {}",
                sessionId, (authentication != null ? authentication.getName() : "NULL"),
                (oauth2User != null ? oauth2User.getName() : "NULL"));

        if (authentication == null || !authentication.isAuthenticated() || oauth2User == null) {
            log.warn("[DEBUG] GET /api/user/me - SecurityContext does NOT contain an authenticated user. Returning 401.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            User user = authService.getAuthenticatedUser(oauth2User);
            log.info("[DEBUG] GET /api/user/me - Found user in DB ID: {}, Email: {}, Role: {}, Username: {}",
                    user.getId(), user.getEmail(), user.getRole(), user.getUsername());
            return ResponseEntity.ok(UserResponse.fromEntity(user));
        } catch (SecurityException e) {
            log.warn("[DEBUG] GET /api/user/me - Authenticated user not found or unauthenticated. Returning 401.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Endpoint to check whether the authenticated user has set up a platform username/roll number.
     */
    @GetMapping("/username/status")
    public ResponseEntity<UsernameStatusResponse> getUsernameStatus(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            User user = authService.getAuthenticatedUser(oauth2User);
            boolean isSet = user.getUsername() != null && !user.getUsername().trim().isEmpty();
            return ResponseEntity.ok(new UsernameStatusResponse(isSet, user.getUsername()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Endpoint to set the platform username/roll number for the currently authenticated user.
     */
    @PostMapping("/username")
    public ResponseEntity<SetUsernameResponse> setUsername(@RequestBody SetUsernameRequest request,
                                                            @AuthenticationPrincipal OAuth2User oauth2User) {
        User user;
        try {
            user = authService.getAuthenticatedUser(oauth2User);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SetUsernameResponse.error("Unauthenticated request"));
        }

        if (request == null || request.username() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SetUsernameResponse.error("Username cannot be empty"));
        }

        String trimmed = request.username().trim();

        if (trimmed.isEmpty() || !USERNAME_PATTERN.matcher(trimmed).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SetUsernameResponse.error("Invalid username format. Username must be 3-30 characters containing letters, numbers, hyphens, or underscores."));
        }

        Optional<User> existingWithUsername = userRepository.findByUsername(trimmed);
        if (existingWithUsername.isPresent() && !existingWithUsername.get().getId().equals(user.getId())) {
            log.warn("[DEBUG] Username '{}' is already taken by user ID: {}", trimmed, existingWithUsername.get().getId());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(SetUsernameResponse.error("Username is already taken."));
        }

        user.setUsername(trimmed);
        userRepository.save(user);
        log.info("[DEBUG] Successfully saved username '{}' for user ID: {}", trimmed, user.getId());

        return ResponseEntity.ok(SetUsernameResponse.success(trimmed));
    }
}
