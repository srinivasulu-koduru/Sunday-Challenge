package com.sundaychallenge.service;

import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service helper to resolve the authenticated User entity from the current Spring Security session.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolves the current logged-in User entity.
     * Throws SecurityException / IllegalArgumentException if unauthenticated.
     */
    public User getAuthenticatedUser(OAuth2User oauth2User) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User is unauthenticated");
        }

        if (oauth2User == null && authentication.getPrincipal() instanceof OAuth2User principal) {
            oauth2User = principal;
        }

        if (oauth2User == null) {
            throw new SecurityException("Authenticated principal is not an OAuth2 user");
        }

        String googleId = (String) oauth2User.getAttributes().get("sub");
        String email = (String) oauth2User.getAttributes().get("email");

        Optional<User> userOpt = Optional.empty();
        if (googleId != null) {
            userOpt = userRepository.findByGoogleId(googleId);
        }
        if (userOpt.isEmpty() && email != null) {
            userOpt = userRepository.findByEmail(email);
        }

        return userOpt.orElseThrow(() -> new SecurityException("Authenticated user not found in database"));
    }
}
