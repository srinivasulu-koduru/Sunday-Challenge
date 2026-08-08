package com.sundaychallenge.controller;

import com.sundaychallenge.dto.UserResponse;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.UserRepository;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * UserController provides user profile management endpoints.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Endpoint to fetch the currently authenticated user's profile.
     * 
     * @param request HttpServletRequest for session tracking
     * @param oauth2User Authenticated OAuth2 principal
     * @return ResponseEntity with UserResponse DTO, or 401 Unauthorized
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

        String googleId = (String) oauth2User.getAttributes().get("sub");
        String email = (String) oauth2User.getAttributes().get("email");

        log.info("[DEBUG] GET /api/user/me - Authenticated user claims sub: {}, email: {}", googleId, email);

        Optional<User> userOpt = userRepository.findByGoogleId(googleId);
        if (userOpt.isEmpty() && email != null) {
            userOpt = userRepository.findByEmail(email);
        }

        return userOpt
                .map(user -> {
                    log.info("[DEBUG] GET /api/user/me - Found user in DB ID: {}, Email: {}, Role: {}",
                            user.getId(), user.getEmail(), user.getRole());
                    return ResponseEntity.ok(UserResponse.fromEntity(user));
                })
                .orElseGet(() -> {
                    log.warn("[DEBUG] GET /api/user/me - Authenticated user not found in DB. Returning 401.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                });
    }
}
