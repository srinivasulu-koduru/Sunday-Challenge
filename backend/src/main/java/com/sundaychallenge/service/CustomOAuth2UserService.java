package com.sundaychallenge.service;

import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom OIDC User Service that intercepts Google OpenID Connect authentication,
 * extracts user claims, and provisions or updates user records in MySQL.
 */
@Service
public class CustomOAuth2UserService extends OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final Set<String> adminEmails;

    public CustomOAuth2UserService(UserRepository userRepository,
                                  @Value("${app.admin.emails:244g1a05cp@srit.ac.in}") String adminEmailsConfig) {
        this.userRepository = userRepository;
        this.adminEmails = Arrays.stream(adminEmailsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        log.info("[DEBUG] Configured Admin Emails: {}", this.adminEmails);
    }

    private boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase());
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String googleId = (String) attributes.get("sub");
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String picture = (String) attributes.get("picture");

        log.info("[DEBUG] CustomOAuth2UserService.loadUser (OIDC) called. googleId: {}, email: {}, name: {}", googleId, email, name);

        if (email == null || email.isBlank()) {
            log.error("[DEBUG] Email not provided by Google OAuth2 provider.");
            throw new OAuth2AuthenticationException("Email not provided by Google OAuth2 provider");
        }

        User user = processOAuthUser(googleId, name, email, picture);

        log.info("[DEBUG] User processed successfully in DB. ID: {}, Email: {}, Role: {}", user.getId(), user.getEmail(), user.getRole());

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new DefaultOidcUser(
                Collections.singleton(authority),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "sub"
        );
    }

    /**
     * Finds existing user by googleId or email, updates details and role if matched as admin email,
     * or creates a new user with ADMIN or STUDENT role depending on email configuration.
     */
    private User processOAuthUser(String googleId, String name, String email, String picture) {
        Role targetRole = isAdminEmail(email) ? Role.ADMIN : Role.STUDENT;

        Optional<User> userByGoogleId = userRepository.findByGoogleId(googleId);

        if (userByGoogleId.isPresent()) {
            User existingUser = userByGoogleId.get();
            log.info("[DEBUG] Existing user found by Google ID: {}", email);
            existingUser.setName(name);
            existingUser.setProfileImage(picture);
            if (isAdminEmail(email)) {
                existingUser.setRole(Role.ADMIN);
            }
            return userRepository.save(existingUser);
        }

        // Account linking: Check if user exists by email without googleId attached
        Optional<User> userByEmail = userRepository.findByEmail(email);

        if (userByEmail.isPresent()) {
            User existingUser = userByEmail.get();
            log.info("[DEBUG] Linking Google ID to existing user account with email: {}", email);
            if (existingUser.getGoogleId() == null) {
                existingUser.setGoogleId(googleId);
            }
            existingUser.setName(name);
            existingUser.setProfileImage(picture);
            if (isAdminEmail(email)) {
                existingUser.setRole(Role.ADMIN);
            }
            return userRepository.save(existingUser);
        }

        // Create new user (ADMIN if email matches configured admin emails, otherwise STUDENT)
        log.info("[DEBUG] Creating new user for email: {} with role: {}", email, targetRole);
        User newUser = new User(googleId, name, email, picture, targetRole);
        User savedUser = userRepository.save(newUser);
        log.info("[DEBUG] Created and saved new user ID: {} for email: {}, Role: {}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
        return savedUser;
    }
}
