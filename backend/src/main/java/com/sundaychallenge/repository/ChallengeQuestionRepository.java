package com.sundaychallenge.repository;

import com.sundaychallenge.entity.ChallengeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeQuestionRepository extends JpaRepository<ChallengeQuestion, Long> {
    
    List<ChallengeQuestion> findByChallengeIdOrderByQuestionOrderAsc(Long challengeId);
}
