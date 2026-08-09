package com.sundaychallenge.repository;

import com.sundaychallenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    
    List<Challenge> findByActiveTrue();

    Optional<Challenge> findByIdAndActiveTrue(Long id);

    Optional<Challenge> findByTitle(String title);
}
