package com.sundaychallenge.service;

import com.sundaychallenge.dto.AnswerReviewResponse;
import com.sundaychallenge.dto.ChallengeDetailsResponse;
import com.sundaychallenge.dto.ChallengeResultResponse;
import com.sundaychallenge.dto.ChallengeSummaryResponse;
import com.sundaychallenge.dto.StartChallengeResponse;
import com.sundaychallenge.dto.SubmitChallengeRequest;
import com.sundaychallenge.dto.SubmitChallengeResponse;
import com.sundaychallenge.dto.UserStatsResponse;
import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.ChallengeQuestion;
import com.sundaychallenge.entity.Question;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.repository.AttemptAnswerRepository;
import com.sundaychallenge.repository.AttemptRepository;
import com.sundaychallenge.repository.ChallengeQuestionRepository;
import com.sundaychallenge.repository.ChallengeRepository;
import com.sundaychallenge.repository.QuestionRepository;
import com.sundaychallenge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ChallengeServiceTest {

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ChallengeQuestionRepository challengeQuestionRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Challenge testChallenge;
    private Question q1;
    private Question q2;

    @BeforeEach
    void setUp() {
        attemptAnswerRepository.deleteAll();
        attemptRepository.deleteAll();
        challengeQuestionRepository.deleteAll();
        challengeRepository.deleteAll();
        questionRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(new User("google-test-user", "Test Student", "student@test.com", "http://image.url", Role.STUDENT));

        testChallenge = new Challenge("Aptitude Unit Test Challenge", "Description for unit test", Category.APTITUDE, Difficulty.EASY, 15, 2, 20, true);
        testChallenge = challengeRepository.save(testChallenge);

        q1 = questionRepository.save(new Question("What is 2 + 2?", "3", "4", "5", "6", "B", 10, "2+2 equals 4."));
        q2 = questionRepository.save(new Question("What is 5 * 5?", "10", "20", "25", "30", "C", 10, "5*5 equals 25."));

        challengeQuestionRepository.save(new ChallengeQuestion(testChallenge, q1, 1));
        challengeQuestionRepository.save(new ChallengeQuestion(testChallenge, q2, 2));
    }

    @Test
    void getActiveChallenges_ShouldReturnActiveChallenges() {
        List<ChallengeSummaryResponse> active = challengeService.getActiveChallenges();
        assertNotNull(active);
        assertFalse(active.isEmpty());
        assertEquals(testChallenge.getTitle(), active.get(0).title());
    }

    @Test
    void getChallengeDetails_ShouldReturnDetailsAndRules() {
        ChallengeDetailsResponse details = challengeService.getChallengeDetails(testChallenge.getId());
        assertNotNull(details);
        assertEquals(testChallenge.getTitle(), details.title());
        assertFalse(details.rules().isEmpty());
    }

    @Test
    void startChallenge_ShouldCreateAttemptAndReturnQuestionsWithoutCorrectAnswers() {
        StartChallengeResponse response = challengeService.startChallenge(testChallenge.getId(), testUser);
        assertNotNull(response);
        assertNotNull(response.attemptId());
        assertEquals(2, response.questions().size());

        // SECURITY CHECK: QuestionResponse MUST NOT expose correctOption or explanation!
        var qResp = response.questions().get(0);
        assertEquals(q1.getQuestionText(), qResp.questionText());
        assertEquals(q1.getOptionA(), qResp.optionA());
    }

    @Test
    void submitChallenge_ShouldCalculateAuthoritativeScoreCorrectly() {
        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), testUser);

        // Submit Q1 correct ("B"), Q2 wrong ("A")
        SubmitChallengeRequest submitReq = new SubmitChallengeRequest(
                startResp.attemptId(),
                Map.of(q1.getId(), "B", q2.getId(), "A")
        );

        SubmitChallengeResponse result = challengeService.submitChallenge(testChallenge.getId(), submitReq, testUser);

        assertNotNull(result);
        assertEquals(10, result.score());
        assertEquals(20, result.totalPoints());
        assertEquals(50.0, result.percentage());
        assertEquals(1, result.correctAnswers());
        assertEquals(1, result.wrongAnswers());
        assertEquals(0, result.unansweredAnswers());
        assertEquals(AttemptStatus.COMPLETED, result.status());
    }

    @Test
    void submitChallenge_WhenUnansweredQuestions_ShouldCountUnanswered() {
        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), testUser);

        // Submit Q1 correct ("B"), leave Q2 unanswered
        SubmitChallengeRequest submitReq = new SubmitChallengeRequest(
                startResp.attemptId(),
                Map.of(q1.getId(), "B")
        );

        SubmitChallengeResponse result = challengeService.submitChallenge(testChallenge.getId(), submitReq, testUser);

        assertNotNull(result);
        assertEquals(10, result.score());
        assertEquals(1, result.correctAnswers());
        assertEquals(0, result.wrongAnswers());
        assertEquals(1, result.unansweredAnswers());
    }

    @Test
    void getAnswerReview_ShouldIncludeCorrectAnswersAndExplanationsAfterSubmission() {
        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), testUser);
        SubmitChallengeRequest submitReq = new SubmitChallengeRequest(startResp.attemptId(), Map.of(q1.getId(), "B", q2.getId(), "A"));
        challengeService.submitChallenge(testChallenge.getId(), submitReq, testUser);

        List<AnswerReviewResponse> review = challengeService.getAnswerReview(testChallenge.getId(), startResp.attemptId(), testUser);
        assertNotNull(review);
        assertEquals(2, review.size());

        AnswerReviewResponse rev1 = review.get(0);
        assertEquals("B", rev1.correctOption());
        assertEquals("B", rev1.selectedOption());
        assertTrue(rev1.correct());
        assertNotNull(rev1.explanation());

        AnswerReviewResponse rev2 = review.get(1);
        assertEquals("C", rev2.correctOption());
        assertEquals("A", rev2.selectedOption());
        assertFalse(rev2.correct());
    }

    @Test
    void getAttemptResult_WhenUnauthorizedUser_ShouldThrowException() {
        User otherUser = userRepository.save(new User("google-other", "Other User", "other@test.com", null, Role.STUDENT));
        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), testUser);

        assertThrows(ResponseStatusException.class, () -> {
            challengeService.getAttemptResult(testChallenge.getId(), startResp.attemptId(), otherUser);
        });
    }

    @Test
    void getUserDashboardStats_ShouldReturnCorrectMetrics() {
        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), testUser);
        SubmitChallengeRequest submitReq = new SubmitChallengeRequest(startResp.attemptId(), Map.of(q1.getId(), "B", q2.getId(), "C"));
        challengeService.submitChallenge(testChallenge.getId(), submitReq, testUser);

        UserStatsResponse stats = challengeService.getUserDashboardStats(testUser);
        assertNotNull(stats);
        assertEquals(1L, stats.completedChallenges());
        assertEquals(20, stats.totalPoints());
    }
}
