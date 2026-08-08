package com.sundaychallenge.controller;

import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void getCurrentUser_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_WhenAuthenticated_ShouldReturn200WithUserProfile() throws Exception {
        // Seed user in database
        User user = new User("google-sub-999", "Verified Student", "verified@gmail.com", "http://profile.pic", Role.STUDENT);
        userRepository.save(user);

        // Construct OAuth2 principal matching seeded user
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-999",
                "name", "Verified Student",
                "email", "verified@gmail.com",
                "picture", "http://profile.pic"
        );
        OAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")),
                attributes,
                "sub"
        );

        mockMvc.perform(get("/api/user/me")
                        .with(SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(oAuth2User)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Verified Student"))
                .andExpect(jsonPath("$.email").value("verified@gmail.com"))
                .andExpect(jsonPath("$.profileImage").value("http://profile.pic"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
