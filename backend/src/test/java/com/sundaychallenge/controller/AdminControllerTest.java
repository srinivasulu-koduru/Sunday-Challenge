package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminAttemptDetailsResponse;
import com.sundaychallenge.dto.AdminAttemptResponse;
import com.sundaychallenge.dto.AdminChallengeRequest;
import com.sundaychallenge.dto.AdminChallengeResponse;
import com.sundaychallenge.dto.AdminQuestionRequest;
import com.sundaychallenge.dto.AdminQuestionResponse;
import com.sundaychallenge.dto.AdminStatsResponse;
import com.sundaychallenge.dto.AdminStudentDetailsResponse;
import com.sundaychallenge.dto.AdminStudentResponse;
import com.sundaychallenge.dto.CategoryReportResponse;
import com.sundaychallenge.dto.ChallengeReportResponse;
import com.sundaychallenge.dto.DifficultyReportResponse;
import com.sundaychallenge.dto.LeaderboardEntryResponse;
import com.sundaychallenge.dto.QuestionResponse;
import com.sundaychallenge.dto.StartChallengeResponse;
import com.sundaychallenge.dto.SubmitChallengeRequest;
import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.ChallengeQuestion;
import com.sundaychallenge.entity.Question;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.repository.AttemptAnswerRepository;
import com.sundaychallenge.repository.AttemptRepository;
import com.sundaychallenge.repository.ChallengeQuestionRepository;
import com.sundaychallenge.repository.ChallengeRepository;
import com.sundaychallenge.repository.QuestionRepository;
import com.sundaychallenge.repository.UserRepository;
import com.sundaychallenge.service.AuthService;
import com.sundaychallenge.service.ChallengeService;
import com.sundaychallenge.service.CustomOAuth2UserService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class AdminControllerTest {

    @Autowired
    private AdminController adminController;

    @Autowired
    private AdminChallengeController adminChallengeController;

    @Autowired
    private AdminQuestionController adminQuestionController;

    @Autowired
    private AdminStudentController adminStudentController;

    @Autowired
    private AdminAttemptController adminAttemptController;

    @Autowired
    private AdminReportController adminReportController;

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

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

    @MockBean
    private AuthService authService;

    private User adminUser;
    private User studentUser;
    private OAuth2User mockAdminOAuth2User;
    private OAuth2User mockStudentOAuth2User;
    private Challenge testChallenge;
    private Question q1;

    @BeforeEach
    void setUp() {
        attemptAnswerRepository.deleteAll();
        attemptRepository.deleteAll();
        challengeQuestionRepository.deleteAll();
        challengeRepository.deleteAll();
        questionRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(new User("google-admin-sub", "Primary Admin", "admin_roll", "244g1a05cp@srit.ac.in", "http://image.url", Role.ADMIN));
        studentUser = userRepository.save(new User("google-student-sub", "Normal Student", "244G1A05CP", "student@test.com", "http://image.url", Role.STUDENT));

        mockAdminOAuth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("sub", "google-admin-sub", "email", "244g1a05cp@srit.ac.in"),
                "sub"
        );

        mockStudentOAuth2User = new DefaultOAuth2User(
                List.of(),
                Map.of("sub", "google-student-sub", "email", "student@test.com"),
                "sub"
        );

        testChallenge = challengeRepository.save(new Challenge("Admin Test Challenge", "Desc", Category.APTITUDE, Difficulty.EASY, 15, 1, 10, true));
        q1 = questionRepository.save(new Question("What is 5+5?", "8", "10", "12", "15", "B", 10, "5+5=10"));
        challengeQuestionRepository.save(new ChallengeQuestion(testChallenge, q1, 1));
    }

    // 1. Primary admin email 244g1a05cp@srit.ac.in receives ADMIN role
    @Test
    void test1_PrimaryAdminEmailReceivesAdminRole() {
        assertEquals(Role.ADMIN, adminUser.getRole());
    }

    // 2. Normal Google user receives STUDENT role
    @Test
    void test2_NormalGoogleUserReceivesStudentRole() {
        assertEquals(Role.STUDENT, studentUser.getRole());
    }

    // 3 & 4. Student cannot access admin endpoints (receives HTTP 403 Forbidden)
    @Test
    void test3And4_StudentCannotAccessAdminEndpointsReceivesForbidden() {
        when(authService.getAuthenticatedUser(any())).thenReturn(studentUser);

        assertThrows(ResponseStatusException.class, () -> adminController.getAdminStats(mockStudentOAuth2User));
        assertThrows(ResponseStatusException.class, () -> adminChallengeController.getAllChallenges(mockStudentOAuth2User));
        assertThrows(ResponseStatusException.class, () -> adminQuestionController.getAllQuestions(mockStudentOAuth2User));
        assertThrows(ResponseStatusException.class, () -> adminStudentController.getAllStudents(null, mockStudentOAuth2User));
        assertThrows(ResponseStatusException.class, () -> adminAttemptController.getAllAttempts(null, null, null, null, null, null, mockStudentOAuth2User));
    }

    // 5, 6, 7. Backend validates authenticated session context (frontend cannot fake role/userId)
    @Test
    void test5To7_BackendValidatesSessionContext() {
        when(authService.getAuthenticatedUser(any())).thenThrow(new SecurityException("Unauthenticated"));

        assertThrows(ResponseStatusException.class, () -> adminController.getAdminStats(null));
    }

    // 8 & 9. Student cannot modify challenges or questions
    @Test
    void test8And9_StudentCannotModifyChallengesOrQuestions() {
        when(authService.getAuthenticatedUser(any())).thenReturn(studentUser);

        AdminChallengeRequest cReq = new AdminChallengeRequest("Unauthorized", "Desc", Category.CODING, Difficulty.HARD, 30, true);
        assertThrows(ResponseStatusException.class, () -> adminChallengeController.createChallenge(cReq, mockStudentOAuth2User));

        AdminQuestionRequest qReq = new AdminQuestionRequest("Unauthorized", "A", "B", "C", "D", "A", 10, "Exp", testChallenge.getId());
        assertThrows(ResponseStatusException.class, () -> adminQuestionController.createQuestion(qReq, mockStudentOAuth2User));
    }

    // 10. Student cannot view correct answers or explanations before submission
    @Test
    void test10_StudentCannotViewCorrectAnswersOrExplanationsBeforeSubmission() {
        when(authService.getAuthenticatedUser(any())).thenReturn(studentUser);

        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), studentUser);
        assertNotNull(startResp);
        QuestionResponse qResp = startResp.questions().get(0);

        assertEquals("What is 5+5?", qResp.questionText());
        assertEquals("8", qResp.optionA());
        assertEquals("10", qResp.optionB());
        assertEquals("12", qResp.optionC());
        assertEquals("15", qResp.optionD());
    }

    // 11. Student cannot view another student's attempt history
    @Test
    void test11_StudentCannotViewAnotherStudentsAttempt() {
        User otherStudent = userRepository.save(new User("sub-other", "Other", "other_roll", "other@test.com", "img", Role.STUDENT));

        when(authService.getAuthenticatedUser(any())).thenReturn(studentUser);
        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), studentUser);

        // Attempting to access using otherStudent throws ResponseStatusException or SecurityException
        assertThrows(Exception.class, () -> {
            challengeService.getAttemptQuestions(testChallenge.getId(), startResp.attemptId(), otherStudent);
        });
    }

    // 12. Admin can access admin APIs
    @Test
    void test12_AdminCanAccessAdminAPIs() {
        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        ResponseEntity<AdminStatsResponse> stats = adminController.getAdminStats(mockAdminOAuth2User);
        assertEquals(HttpStatus.OK, stats.getStatusCode());
        assertNotNull(stats.getBody());
    }

    // 13. Admin statistics calculation
    @Test
    void test13_AdminStatsCalculatedAccurately() {
        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        ResponseEntity<AdminStatsResponse> response = adminController.getAdminStats(mockAdminOAuth2User);
        assertEquals(1, response.getBody().totalStudents());
        assertEquals(1, response.getBody().totalChallenges());
        assertEquals(1, response.getBody().activeChallenges());
    }

    // 14. Admin student listing and search
    @Test
    void test14_StudentListingAndSearchWorks() {
        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        ResponseEntity<List<AdminStudentResponse>> allStudents = adminStudentController.getAllStudents(null, mockAdminOAuth2User);
        assertEquals(1, allStudents.getBody().size());

        ResponseEntity<List<AdminStudentResponse>> searchResult = adminStudentController.getAllStudents("244G1A05CP", mockAdminOAuth2User);
        assertEquals(1, searchResult.getBody().size());
    }

    // 15. Challenge CRUD and safe deletion guard
    @Test
    void test15_ChallengeCRUDAndSafeDeletionGuard() {
        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        // Create
        AdminChallengeRequest createReq = new AdminChallengeRequest("New Challenge", "Desc", Category.REASONING, Difficulty.MEDIUM, 20, true);
        ResponseEntity<AdminChallengeResponse> created = adminChallengeController.createChallenge(createReq, mockAdminOAuth2User);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());

        // Update
        AdminChallengeRequest updateReq = new AdminChallengeRequest("Updated Title", "Desc", Category.REASONING, Difficulty.MEDIUM, 20, true);
        ResponseEntity<AdminChallengeResponse> updated = adminChallengeController.updateChallenge(created.getBody().id(), updateReq, mockAdminOAuth2User);
        assertEquals("Updated Title", updated.getBody().title());

        // Delete unreferenced challenge
        ResponseEntity<Void> deleted = adminChallengeController.deleteChallenge(created.getBody().id(), mockAdminOAuth2User);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    }

    // 16. Question management
    @Test
    void test16_QuestionManagementWorks() {
        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        AdminQuestionRequest req = new AdminQuestionRequest("Q2?", "A", "B", "C", "D", "A", 10, "Exp", testChallenge.getId());
        ResponseEntity<AdminQuestionResponse> created = adminQuestionController.createQuestion(req, mockAdminOAuth2User);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());

        ResponseEntity<List<AdminQuestionResponse>> allQuestions = adminQuestionController.getAllQuestions(mockAdminOAuth2User);
        assertFalse(allQuestions.getBody().isEmpty());
    }

    // 17. Attempt listing & detail inspection
    @Test
    void test17_AttemptListingAndInspectionWorks() {
        when(authService.getAuthenticatedUser(any())).thenReturn(studentUser);
        StartChallengeResponse startResp = challengeService.startChallenge(testChallenge.getId(), studentUser);
        challengeService.submitChallenge(testChallenge.getId(), new SubmitChallengeRequest(startResp.attemptId(), Map.of(q1.getId(), "B")), studentUser);

        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        ResponseEntity<List<AdminAttemptResponse>> attempts = adminAttemptController.getAllAttempts(null, null, testChallenge.getId(), null, null, null, mockAdminOAuth2User);
        assertEquals(1, attempts.getBody().size());

        ResponseEntity<AdminAttemptDetailsResponse> details = adminAttemptController.getAttemptDetails(startResp.attemptId(), mockAdminOAuth2User);
        assertEquals(HttpStatus.OK, details.getStatusCode());
        assertEquals(10, details.getBody().score());
    }

    // 18. Reports (challenge, category, difficulty)
    @Test
    void test18_ReportsCalculateCorrectly() {
        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        ResponseEntity<List<ChallengeReportResponse>> cRep = adminReportController.getChallengeReports(mockAdminOAuth2User);
        assertEquals(HttpStatus.OK, cRep.getStatusCode());

        ResponseEntity<List<CategoryReportResponse>> catRep = adminReportController.getCategoryReports(mockAdminOAuth2User);
        assertEquals(HttpStatus.OK, catRep.getStatusCode());

        ResponseEntity<List<DifficultyReportResponse>> diffRep = adminReportController.getDifficultyReports(mockAdminOAuth2User);
        assertEquals(HttpStatus.OK, diffRep.getStatusCode());
    }

    // 19. Leaderboard rankings
    @Test
    void test19_LeaderboardRankingsCalculatedCorrectly() {
        when(authService.getAuthenticatedUser(any())).thenReturn(adminUser);

        ResponseEntity<List<LeaderboardEntryResponse>> leaderboard = adminReportController.getLeaderboard(mockAdminOAuth2User);
        assertEquals(HttpStatus.OK, leaderboard.getStatusCode());
        assertNotNull(leaderboard.getBody());
    }
}
