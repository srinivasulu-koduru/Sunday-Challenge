package com.sundaychallenge.service;

import com.sundaychallenge.dto.AnswerReviewResponse;
import com.sundaychallenge.dto.ChallengeDetailsResponse;
import com.sundaychallenge.dto.ChallengeResultResponse;
import com.sundaychallenge.dto.ChallengeSummaryResponse;
import com.sundaychallenge.dto.QuestionResponse;
import com.sundaychallenge.dto.StartChallengeResponse;
import com.sundaychallenge.dto.SubmitChallengeRequest;
import com.sundaychallenge.dto.SubmitChallengeResponse;
import com.sundaychallenge.dto.UserAttemptSummaryResponse;
import com.sundaychallenge.dto.UserStatsResponse;
import com.sundaychallenge.entity.Attempt;
import com.sundaychallenge.entity.AttemptAnswer;
import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.ChallengeQuestion;
import com.sundaychallenge.entity.Question;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.repository.AttemptAnswerRepository;
import com.sundaychallenge.repository.AttemptRepository;
import com.sundaychallenge.repository.ChallengeQuestionRepository;
import com.sundaychallenge.repository.ChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ChallengeService handling active challenges, attempt execution, authoritative server-side scoring,
 * time verification, and results/review reporting.
 */
@Service
public class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);

    private final ChallengeRepository challengeRepository;
    private final ChallengeQuestionRepository challengeQuestionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                            ChallengeQuestionRepository challengeQuestionRepository,
                            AttemptRepository attemptRepository,
                            AttemptAnswerRepository attemptAnswerRepository) {
        this.challengeRepository = challengeRepository;
        this.challengeQuestionRepository = challengeQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
    }

    /**
     * Returns list of all active challenges.
     */
    @Transactional(readOnly = true)
    public List<ChallengeSummaryResponse> getActiveChallenges() {
        return challengeRepository.findByActiveTrue().stream()
                .map(ChallengeSummaryResponse::fromEntity)
                .toList();
    }

    /**
     * Returns detailed metadata for a specific challenge.
     */
    @Transactional(readOnly = true)
    public ChallengeDetailsResponse getChallengeDetails(Long challengeId) {
        Challenge challenge = challengeRepository.findByIdAndActiveTrue(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found or inactive"));
        return ChallengeDetailsResponse.fromEntity(challenge);
    }

    /**
     * Starts a new challenge attempt for the authenticated user, or reuses an existing active IN_PROGRESS attempt.
     */
    @Transactional
    public StartChallengeResponse startChallenge(Long challengeId, User user) {
        Challenge challenge = challengeRepository.findByIdAndActiveTrue(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found or inactive"));

        // Authoritative Schedule Window Checks
        LocalDateTime now = LocalDateTime.now();
        if (challenge.getStartTime() != null && now.isBefore(challenge.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge is upcoming and not yet available for attempts");
        }
        if (challenge.getEndTime() != null && now.isAfter(challenge.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge availability window has ended");
        }

        // Reuse existing IN_PROGRESS attempt if user has one for this challenge
        List<Attempt> inProgressAttempts = attemptRepository.findByUserIdAndStatus(user.getId(), AttemptStatus.IN_PROGRESS);
        Optional<Attempt> existingActiveOpt = inProgressAttempts.stream()
                .filter(a -> a.getChallenge().getId().equals(challengeId))
                .findFirst();

        if (existingActiveOpt.isPresent()) {
            Attempt existingAttempt = existingActiveOpt.get();
            log.info("[DEBUG] Reusing existing IN_PROGRESS attempt ID: {} for user ID: {} on challenge ID: {}",
                    existingAttempt.getId(), user.getId(), challengeId);
            return buildStartChallengeResponse(existingAttempt);
        }

        Attempt attempt = new Attempt(user, challenge, LocalDateTime.now(), AttemptStatus.IN_PROGRESS);
        attempt = attemptRepository.save(attempt);

        log.info("[DEBUG] Started new attempt ID: {} for user ID: {} on challenge ID: {}",
                attempt.getId(), user.getId(), challenge.getId());

        return buildStartChallengeResponse(attempt);
    }

    /**
     * Fetches questions for an existing IN_PROGRESS attempt without creating a new attempt.
     */
    @Transactional(readOnly = true)
    public StartChallengeResponse getAttemptQuestions(Long challengeId, Long attemptId, User user) {
        Attempt attempt = attemptRepository.findByIdAndUserIdAndChallengeId(attemptId, user.getId(), challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found for user and challenge"));

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            log.warn("[DEBUG] Attempt ID: {} status is {}. Cannot fetch active questions.", attemptId, attempt.getStatus());
        }

        return buildStartChallengeResponse(attempt);
    }

    private StartChallengeResponse buildStartChallengeResponse(Attempt attempt) {
        List<ChallengeQuestion> cqList = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(attempt.getChallenge().getId());
        List<QuestionResponse> questions = cqList.stream()
                .map(cq -> QuestionResponse.fromEntity(cq.getQuestion(), cq.getQuestionOrder()))
                .toList();

        return new StartChallengeResponse(
                attempt.getId(),
                attempt.getChallenge().getId(),
                attempt.getChallenge().getTitle(),
                attempt.getChallenge().getDurationMinutes(),
                attempt.getStartedAt(),
                questions
        );
    }

    /**
     * Authoritatively evaluates and submits a challenge attempt.
     */
    @Transactional
    public SubmitChallengeResponse submitChallenge(Long challengeId, SubmitChallengeRequest request, User user) {
        if (request == null || request.attemptId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt ID is required");
        }

        Attempt attempt = attemptRepository.findByIdAndUserIdAndChallengeId(request.attemptId(), user.getId(), challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found for user and challenge"));

        // If attempt is already completed or expired, return existing evaluated result
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            log.warn("[DEBUG] Attempt ID: {} is already in status: {}. Returning saved result.", attempt.getId(), attempt.getStatus());
            long timeTaken = Duration.between(attempt.getStartedAt(),
                    attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : LocalDateTime.now()).getSeconds();
            double pct = attempt.getTotalPoints() > 0 ? (double) attempt.getScore() / attempt.getTotalPoints() * 100.0 : 0.0;
            return new SubmitChallengeResponse(
                    attempt.getId(),
                    challengeId,
                    attempt.getChallenge().getTitle(),
                    attempt.getScore(),
                    attempt.getTotalPoints(),
                    Math.round(pct * 100.0) / 100.0,
                    attempt.getCorrectAnswers(),
                    attempt.getWrongAnswers(),
                    attempt.getUnansweredAnswers(),
                    attempt.getPointsEarned(),
                    attempt.getStatus(),
                    attempt.getSubmittedAt(),
                    timeTaken
            );
        }

        Challenge challenge = attempt.getChallenge();
        LocalDateTime now = LocalDateTime.now();

        // Calculate backend time limit (+ 1 min grace period for network latency)
        LocalDateTime maxAllowedTime = attempt.getStartedAt().plusMinutes(challenge.getDurationMinutes()).plusMinutes(1);
        boolean isExpired = now.isAfter(maxAllowedTime);

        AttemptStatus finalStatus = isExpired ? AttemptStatus.EXPIRED : AttemptStatus.COMPLETED;

        // Perform authoritative server-side score calculation
        List<ChallengeQuestion> cqList = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(challengeId);
        Map<Long, String> submittedAnswers = request.answers() != null ? request.answers() : Map.of();

        int score = 0;
        int totalPoints = 0;
        int correctCount = 0;
        int wrongCount = 0;
        int unansweredCount = 0;

        for (ChallengeQuestion cq : cqList) {
            Question question = cq.getQuestion();
            int qPoints = question.getPoints();
            totalPoints += qPoints;

            String selectedOption = submittedAnswers.get(question.getId());
            boolean isAnswered = selectedOption != null && !selectedOption.trim().isEmpty();

            boolean isCorrect = false;
            int pointsEarned = 0;

            if (isAnswered) {
                if (question.getCorrectOption().equalsIgnoreCase(selectedOption.trim())) {
                    isCorrect = true;
                    correctCount++;
                    score += qPoints;
                    pointsEarned = qPoints;
                } else {
                    wrongCount++;
                }
            } else {
                unansweredCount++;
            }

            AttemptAnswer attemptAnswer = new AttemptAnswer(attempt, question, selectedOption, isCorrect, pointsEarned);
            attemptAnswerRepository.save(attemptAnswer);
        }

        double percentage = totalPoints > 0 ? (double) score / totalPoints * 100.0 : 0.0;
        double roundedPct = Math.round(percentage * 100.0) / 100.0;

        attempt.setSubmittedAt(now);
        attempt.setStatus(finalStatus);
        attempt.setScore(score);
        attempt.setTotalPoints(totalPoints);
        attempt.setCorrectAnswers(correctCount);
        attempt.setWrongAnswers(wrongCount);
        attempt.setUnansweredAnswers(unansweredCount);
        attempt.setPointsEarned(score); // Points earned equals calculated score

        attempt = attemptRepository.save(attempt);

        long timeTakenSeconds = Duration.between(attempt.getStartedAt(), now).getSeconds();

        log.info("[DEBUG] Submitted attempt ID: {}. Status: {}, Score: {}/{}, TimeTaken: {}s",
                attempt.getId(), finalStatus, score, totalPoints, timeTakenSeconds);

        return new SubmitChallengeResponse(
                attempt.getId(),
                challenge.getId(),
                challenge.getTitle(),
                score,
                totalPoints,
                roundedPct,
                correctCount,
                wrongCount,
                unansweredCount,
                score,
                finalStatus,
                now,
                timeTakenSeconds
        );
    }

    /**
     * Retrieves evaluated result summary for a specific attempt.
     */
    @Transactional(readOnly = true)
    public ChallengeResultResponse getAttemptResult(Long challengeId, Long attemptId, User user) {
        Attempt attempt = attemptRepository.findByIdAndUserIdAndChallengeId(attemptId, user.getId(), challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt result not found or unauthorized"));

        long timeTakenSeconds = Duration.between(attempt.getStartedAt(),
                attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : LocalDateTime.now()).getSeconds();
        double pct = attempt.getTotalPoints() > 0 ? (double) attempt.getScore() / attempt.getTotalPoints() * 100.0 : 0.0;

        return new ChallengeResultResponse(
                attempt.getId(),
                challengeId,
                attempt.getChallenge().getTitle(),
                attempt.getScore(),
                attempt.getTotalPoints(),
                Math.round(pct * 100.0) / 100.0,
                attempt.getCorrectAnswers(),
                attempt.getWrongAnswers(),
                attempt.getUnansweredAnswers(),
                attempt.getPointsEarned(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                timeTakenSeconds
        );
    }

    /**
     * Retrieves full answer review after submission.
     */
    @Transactional(readOnly = true)
    public List<AnswerReviewResponse> getAnswerReview(Long challengeId, Long attemptId, User user) {
        Attempt attempt = attemptRepository.findByIdAndUserIdAndChallengeId(attemptId, user.getId(), challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found or unauthorized"));

        List<ChallengeQuestion> cqList = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(challengeId);
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);

        Map<Long, AttemptAnswer> answerMap = answers.stream()
                .collect(java.util.stream.Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        List<AnswerReviewResponse> reviewList = new ArrayList<>();
        for (ChallengeQuestion cq : cqList) {
            Question question = cq.getQuestion();
            AttemptAnswer aa = answerMap.get(question.getId());

            String selectedOption = (aa != null) ? aa.getSelectedOption() : null;
            boolean isCorrect = (aa != null) && aa.isCorrect();
            int points = (aa != null) ? aa.getPointsEarned() : 0;

            reviewList.add(new AnswerReviewResponse(
                    question.getId(),
                    cq.getQuestionOrder(),
                    question.getQuestionText(),
                    question.getOptionA(),
                    question.getOptionB(),
                    question.getOptionC(),
                    question.getOptionD(),
                    selectedOption,
                    question.getCorrectOption(),
                    isCorrect,
                    points,
                    question.getExplanation()
            ));
        }

        return reviewList;
    }

    /**
     * Retrieves attempt history for the logged-in student.
     */
    @Transactional(readOnly = true)
    public List<UserAttemptSummaryResponse> getUserAttempts(User user) {
        return attemptRepository.findByUserIdOrderByStartedAtDesc(user.getId()).stream()
                .map(a -> {
                    double pct = a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0;
                    return new UserAttemptSummaryResponse(
                            a.getId(),
                            a.getChallenge().getId(),
                            a.getChallenge().getTitle(),
                            a.getChallenge().getCategory(),
                            a.getChallenge().getDifficulty(),
                            a.getScore(),
                            a.getTotalPoints(),
                            Math.round(pct * 100.0) / 100.0,
                            a.getPointsEarned(),
                            a.getStatus(),
                            a.getSubmittedAt()
                    );
                })
                .toList();
    }

    /**
     * Returns aggregated statistics for the student dashboard.
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserDashboardStats(User user) {
        long totalActive = challengeRepository.findByActiveTrue().size();
        long completed = attemptRepository.countByUserIdAndStatus(user.getId(), AttemptStatus.COMPLETED);
        long pending = attemptRepository.countByUserIdAndStatus(user.getId(), AttemptStatus.IN_PROGRESS);
        int points = attemptRepository.sumPointsEarnedByUserId(user.getId());

        return new UserStatsResponse(totalActive, completed, pending, points);
    }
}
