package com.sundaychallenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sundaychallenge.dto.SetUsernameRequest;

import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.UserRepository;
import com.sundaychallenge.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserControllerTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    private User testUser;
    private OAuth2User mockOAuth2User;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = userRepository.save(new User("google-sub-100", "Test Student", "student@test.com", "http://image.png", Role.STUDENT));
        mockOAuth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("sub", "google-sub-100", "email", "student@test.com"),
                "sub"
        );
    }

    // 1. New user has no username
    @Test
    void test1_NewUserHasNoUsername() {
        assertNull(testUser.getUsername());
    }

    // 2. Username status returns false
    @Test
    void test2_UsernameStatusReturnsFalseForNewUser() {
        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        var response = userController.getUsernameStatus(mockOAuth2User);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().usernameSet());
        assertNull(response.getBody().username());
    }

    // 3 & 4. Authenticated user can set username and it is saved in DB
    @Test
    void test3And4_AuthenticatedUserCanSetUsernameAndItIsSavedInDB() {
        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        SetUsernameRequest request = new SetUsernameRequest("244G1A05CP");
        var response = userController.setUsername(request, mockOAuth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals("244G1A05CP", response.getBody().username());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("244G1A05CP", updatedUser.getUsername());
    }

    // 5. Username status returns true after saving
    @Test
    void test5_UsernameStatusReturnsTrueAfterSaving() {
        testUser.setUsername("244G1A05CP");
        userRepository.save(testUser);

        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        var response = userController.getUsernameStatus(mockOAuth2User);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().usernameSet());
        assertEquals("244G1A05CP", response.getBody().username());
    }

    // 6. Duplicate username is rejected with Conflict status (409)
    @Test
    void test6_DuplicateUsernameIsRejectedWithConflictStatus() {
        User otherUser = userRepository.save(new User("google-sub-200", "Other Student", "244G1A05CP", "other@test.com", "http://image.png", Role.STUDENT));

        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        SetUsernameRequest request = new SetUsernameRequest("244G1A05CP");
        var response = userController.setUsername(request, mockOAuth2User);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("Username is already taken.", response.getBody().message());
    }

    // 7. Invalid username format is rejected (400 Bad Request)
    @Test
    void test7_InvalidUsernameIsRejectedWithBadRequestStatus() {
        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        SetUsernameRequest request = new SetUsernameRequest("invalid username with spaces!");
        var response = userController.setUsername(request, mockOAuth2User);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().success());
    }

    // 8. Empty username is rejected (400 Bad Request)
    @Test
    void test8_EmptyUsernameIsRejectedWithBadRequestStatus() {
        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        SetUsernameRequest request = new SetUsernameRequest("   ");
        var response = userController.setUsername(request, mockOAuth2User);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().success());
    }

    // 9. Unauthenticated request is rejected (401 Unauthorized)
    @Test
    void test9_UnauthenticatedRequestIsRejected() {
        when(authService.getAuthenticatedUser(any())).thenThrow(new SecurityException("Unauthenticated"));

        var response = userController.setUsername(new SetUsernameRequest("244G1A05CP"), null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // 10. User cannot update another user's username through request parameters
    @Test
    void test10_UserCannotUpdateAnotherUsersUsernameThroughRequestParameters() {
        User victim = userRepository.save(new User("google-sub-victim", "Victim Student", "victim_old", "victim@test.com", "http://image.png", Role.STUDENT));

        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        SetUsernameRequest request = new SetUsernameRequest("attacker_username");
        userController.setUsername(request, mockOAuth2User);

        User updatedVictim = userRepository.findById(victim.getId()).orElseThrow();
        assertEquals("victim_old", updatedVictim.getUsername());
    }

    // 11. Existing username is not overwritten by duplicate attempt
    @Test
    void test11_ExistingUsernameIsNotOverwrittenByDuplicateAttempt() {
        User existingUser = userRepository.save(new User("google-sub-existing", "Existing Student", "EXISTING_ROLL", "existing@test.com", "http://image.png", Role.STUDENT));

        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        SetUsernameRequest request = new SetUsernameRequest("EXISTING_ROLL");
        var response = userController.setUsername(request, mockOAuth2User);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        User checkExisting = userRepository.findById(existingUser.getId()).orElseThrow();
        assertEquals("EXISTING_ROLL", checkExisting.getUsername());
    }

    // 12. Existing users without username can complete setup
    @Test
    void test12_ExistingUsersWithoutUsernameCanCompleteSetup() {
        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        SetUsernameRequest request = new SetUsernameRequest("SETUP_SUCCESS");
        var response = userController.setUsername(request, mockOAuth2User);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("SETUP_SUCCESS", updated.getUsername());
    }

    // 13. Existing users with username do not need setup again
    @Test
    void test13_ExistingUsersWithUsernameDoNotNeedSetupAgain() {
        testUser.setUsername("ALREADY_SET");
        userRepository.save(testUser);

        when(authService.getAuthenticatedUser(any())).thenReturn(testUser);

        var response = userController.getUsernameStatus(mockOAuth2User);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().usernameSet());
        assertEquals("ALREADY_SET", response.getBody().username());
    }
}
