package com.sundaychallenge.service;

import com.sundaychallenge.dto.LeaderboardPageResponse;
import com.sundaychallenge.dto.LeaderboardUserPositionResponse;
import com.sundaychallenge.entity.Attempt;
import com.sundaychallenge.entity.Challenge;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LeaderboardServiceTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private UserRepository userRepository;

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

    private User student1;
    private User student2;
    private User student3;
    private Challenge challengeAptitude;
    private Challenge challengeReasoning;

    @BeforeEach
    void setUp() {
        attemptAnswerRepository.deleteAll();
        attemptRepository.deleteAll();
        challengeQuestionRepository.deleteAll();
        questionRepository.deleteAll();
        challengeRepository.deleteAll();

        userRepository.findAll().forEach(u -> {
            if (u.getRole() == Role.STUDENT) {
                userRepository.delete(u);
            }
        });
        userRepository.flush();

        // Seed 3 Student users
        student1 = new User("g-stu-1", "Srinivasulu K", "srinivasulu@example.com", "pic1", Role.STUDENT);
        student1.setUsername("244G1A05CS");
        student1 = userRepository.saveAndFlush(student1);

        student2 = new User("g-stu-2", "Ravi Kumar", "ravi@example.com", "pic2", Role.STUDENT);
        student2.setUsername("244G1A06CS");
        student2 = userRepository.saveAndFlush(student2);

        student3 = new User("g-stu-3", "Anil Kumar", "anil@example.com", "pic3", Role.STUDENT);
        student3.setUsername("244G1A07CS");
        student3 = userRepository.saveAndFlush(student3);

        // Seed 2 Challenges
        challengeAptitude = new Challenge("Aptitude Challenge 1", "Desc", Category.APTITUDE, Difficulty.EASY, 15, 0, 0, true);
        challengeAptitude = challengeRepository.saveAndFlush(challengeAptitude);

        challengeReasoning = new Challenge("Reasoning Challenge 1", "Desc", Category.REASONING, Difficulty.HARD, 20, 0, 0, true);
        challengeReasoning = challengeRepository.saveAndFlush(challengeReasoning);
    }

    @Test
    void test01_EmptyLeaderboard() {
        userRepository.deleteAll();
        userRepository.flush();

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        assertNotNull(res);
        assertEquals(0, res.totalStudents());
        assertEquals(0, res.entries().size());
    }

    @Test
    void test02_OneStudentLeaderboard() {
        userRepository.delete(student2);
        userRepository.delete(student3);
        userRepository.flush();

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        assertEquals(1, res.totalStudents());
        assertEquals(1, res.entries().size());
        assertEquals("244G1A05CS", res.entries().get(0).username());
    }

    @Test
    void test03_MultipleStudentsRankingByTotalPoints() {
        // Student 1 scores 80 pts
        Attempt a1 = new Attempt(student1, challengeAptitude);
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(80);
        a1.setPointsEarned(80);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        // Student 2 scores 95 pts
        Attempt a2 = new Attempt(student2, challengeAptitude);
        a2.setStatus(AttemptStatus.COMPLETED);
        a2.setScore(95);
        a2.setPointsEarned(95);
        a2.setTotalPoints(100);
        attemptRepository.saveAndFlush(a2);

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        assertEquals(3, res.totalStudents());
        // Rank 1: Student 2 (95 pts)
        assertEquals("244G1A06CS", res.entries().get(0).username());
        assertEquals(1, res.entries().get(0).rank());
        assertEquals(95, res.entries().get(0).totalPoints());

        // Rank 2: Student 1 (80 pts)
        assertEquals("244G1A05CS", res.entries().get(1).username());
        assertEquals(2, res.entries().get(1).rank());
        assertEquals(80, res.entries().get(1).totalPoints());
    }

    @Test
    void test04_TieBreakingLogic() {
        Attempt a1 = new Attempt(student1, challengeAptitude);
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(50);
        a1.setPointsEarned(50);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        Attempt a2 = new Attempt(student2, challengeAptitude);
        a2.setStatus(AttemptStatus.COMPLETED);
        a2.setScore(50);
        a2.setPointsEarned(50);
        a2.setTotalPoints(100);
        attemptRepository.saveAndFlush(a2);

        Attempt a1_2 = new Attempt(student1, challengeReasoning);
        a1_2.setStatus(AttemptStatus.COMPLETED);
        a1_2.setScore(0);
        a1_2.setPointsEarned(0);
        a1_2.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1_2);

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        assertEquals("244G1A05CS", res.entries().get(0).username());
    }

    @Test
    void test05_AverageScoreAndBestScoreCalculation() {
        Attempt a1 = new Attempt(student1, challengeAptitude);
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(80);
        a1.setPointsEarned(80);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        Attempt a2 = new Attempt(student1, challengeReasoning);
        a2.setStatus(AttemptStatus.COMPLETED);
        a2.setScore(60);
        a2.setPointsEarned(60);
        a2.setTotalPoints(100);
        attemptRepository.saveAndFlush(a2);

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        var entry1 = res.entries().stream().filter(e -> e.userId().equals(student1.getId())).findFirst().orElseThrow();

        assertEquals(80, entry1.bestScore());
        assertEquals(70.0, entry1.averageScore()); // (80 + 60) / 2 = 70.0%
        assertEquals(140, entry1.totalPoints()); // 80 + 60
    }

    @Test
    void test06_MultipleAttemptsOnSameChallengeDoesNotInflateCompletedCount() {
        Attempt a1 = new Attempt(student1, challengeAptitude);
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(50);
        a1.setPointsEarned(50);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        Attempt a2 = new Attempt(student1, challengeAptitude);
        a2.setStatus(AttemptStatus.COMPLETED);
        a2.setScore(90);
        a2.setPointsEarned(90);
        a2.setTotalPoints(100);
        attemptRepository.saveAndFlush(a2);

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        var entry = res.entries().stream().filter(e -> e.userId().equals(student1.getId())).findFirst().orElseThrow();

        assertEquals(1, entry.completedChallenges());
        assertEquals(90, entry.totalPoints());
    }

    @Test
    void test07_ChallengeAndCategoryAndDifficultyFiltering() {
        Attempt a1 = new Attempt(student1, challengeAptitude); // APTITUDE, EASY
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(100);
        a1.setPointsEarned(100);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        Attempt a2 = new Attempt(student2, challengeReasoning); // REASONING, HARD
        a2.setStatus(AttemptStatus.COMPLETED);
        a2.setScore(100);
        a2.setPointsEarned(100);
        a2.setTotalPoints(100);
        attemptRepository.saveAndFlush(a2);

        LeaderboardPageResponse catRes = leaderboardService.getLeaderboardPage(null, Category.APTITUDE, null, "ALL_TIME", null, null, 0, 20, null);
        assertEquals(100, catRes.entries().get(0).totalPoints());
        assertEquals("244G1A05CS", catRes.entries().get(0).username());

        LeaderboardPageResponse diffRes = leaderboardService.getLeaderboardPage(null, null, Difficulty.HARD, "ALL_TIME", null, null, 0, 20, null);
        assertEquals("244G1A06CS", diffRes.entries().get(0).username());
    }

    @Test
    void test08_SearchByNameAndRollNumber() {
        Attempt a1 = new Attempt(student1, challengeAptitude);
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(50);
        a1.setPointsEarned(50);
        attemptRepository.saveAndFlush(a1);

        LeaderboardPageResponse searchRoll = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", "244G1A05CS", null, 0, 20, null);
        assertEquals(1, searchRoll.entries().size());
        assertEquals("Srinivasulu K", searchRoll.entries().get(0).name());

        LeaderboardPageResponse searchName = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", "Ravi", null, 0, 20, null);
        assertEquals(1, searchName.entries().size());
        assertEquals("244G1A06CS", searchName.entries().get(0).username());
    }

    @Test
    void test09_TimePeriodFiltering() {
        Attempt oldAttempt = new Attempt(student1, challengeAptitude);
        oldAttempt.setStatus(AttemptStatus.COMPLETED);
        oldAttempt.setScore(100);
        oldAttempt.setPointsEarned(100);
        oldAttempt.setStartedAt(LocalDateTime.now().minusDays(40));
        attemptRepository.saveAndFlush(oldAttempt);

        Attempt recentAttempt = new Attempt(student2, challengeAptitude);
        recentAttempt.setStatus(AttemptStatus.COMPLETED);
        recentAttempt.setScore(100);
        recentAttempt.setPointsEarned(100);
        recentAttempt.setStartedAt(LocalDateTime.now().minusHours(2));
        attemptRepository.saveAndFlush(recentAttempt);

        LeaderboardPageResponse monthRes = leaderboardService.getLeaderboardPage(null, null, null, "THIS_MONTH", null, null, 0, 20, null);
        assertEquals("244G1A06CS", monthRes.entries().get(0).username());
        assertEquals(100, monthRes.entries().get(0).totalPoints());

        LeaderboardPageResponse allRes = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        assertEquals(2, allRes.entries().stream().filter(e -> e.totalPoints() > 0).count());
    }

    @Test
    void test10_CurrentUserPosition() {
        Attempt a1 = new Attempt(student1, challengeAptitude);
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(100);
        a1.setPointsEarned(100);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        LeaderboardUserPositionResponse pos = leaderboardService.getCurrentUserPosition(student1);
        assertNotNull(pos);
        assertEquals(1, pos.rank());
        assertEquals(100, pos.totalPoints());
        assertEquals("244G1A05CS", pos.username());
    }

    @Test
    void test11_PaginationHandling() {
        LeaderboardPageResponse page0 = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 2, null);
        assertEquals(2, page0.entries().size());
        assertEquals(0, page0.currentPage());

        LeaderboardPageResponse page1 = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 1, 2, null);
        assertEquals(1, page1.entries().size());
        assertEquals(1, page1.currentPage());
    }

    @Test
    void test12_ExpiredAttemptCountedInLeaderboard() {
        Attempt a1 = new Attempt(student1, challengeAptitude);
        a1.setStatus(AttemptStatus.EXPIRED);
        a1.setScore(75);
        a1.setPointsEarned(75);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        var entry1 = res.entries().stream().filter(e -> e.userId().equals(student1.getId())).findFirst().orElseThrow();
        assertEquals(75, entry1.totalPoints());
        assertEquals(1, entry1.completedChallenges());
    }

    @Test
    void test13_HistoricalInactiveChallengeAppearsInLeaderboard() {
        Challenge inactiveChallenge = new Challenge("Old Challenge", "Desc", Category.CODING, Difficulty.MEDIUM, 30, 0, 0, false);
        inactiveChallenge.setActive(false);
        inactiveChallenge = challengeRepository.saveAndFlush(inactiveChallenge);

        Attempt a1 = new Attempt(student1, inactiveChallenge);
        a1.setStatus(AttemptStatus.COMPLETED);
        a1.setScore(90);
        a1.setPointsEarned(90);
        a1.setTotalPoints(100);
        attemptRepository.saveAndFlush(a1);

        LeaderboardPageResponse res = leaderboardService.getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, 20, null);
        var entry1 = res.entries().stream().filter(e -> e.userId().equals(student1.getId())).findFirst().orElseThrow();
        assertEquals(90, entry1.totalPoints());
    }
}
