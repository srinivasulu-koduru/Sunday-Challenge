package com.sundaychallenge.repository;

import com.sundaychallenge.entity.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttemptId(Long attemptId);

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    boolean existsByQuestionId(Long questionId);
}
