package com.sundaychallenge.repository;

import com.sundaychallenge.entity.Attempt;
import com.sundaychallenge.entity.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<Attempt> findByIdAndUserId(Long id, Long userId);

    Optional<Attempt> findByIdAndUserIdAndChallengeId(Long id, Long userId, Long challengeId);

    List<Attempt> findByUserIdAndStatus(Long userId, AttemptStatus status);

    Long countByUserIdAndStatus(Long userId, AttemptStatus status);

    long countByStatus(AttemptStatus status);

    boolean existsByChallengeId(Long challengeId);

    List<Attempt> findByChallengeId(Long challengeId);

    @Query("SELECT COALESCE(SUM(a.pointsEarned), 0) FROM Attempt a WHERE a.user.id = :userId AND (a.status = 'COMPLETED' OR a.status = 'EXPIRED')")
    Integer sumPointsEarnedByUserId(@Param("userId") Long userId);
}
