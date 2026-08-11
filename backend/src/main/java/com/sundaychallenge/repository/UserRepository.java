package com.sundaychallenge.repository;

import com.sundaychallenge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for User persistence operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique Google OAuth2 ID.
     *
     * @param googleId The Google user identifier (sub)
     * @return Optional containing the User if found
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Find a user by their unique email address.
     *
     * @param email The user's email address
     * @return Optional containing the User if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their unique platform username.
     *
     * @param username The platform username / roll number
     * @return Optional containing the User if found
     */
    Optional<User> findByUsername(String username);
}
