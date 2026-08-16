package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminAccessRequest;
import com.sundaychallenge.dto.AdminAccessResponse;
import com.sundaychallenge.dto.NotRegisteredStudentResponse;
import com.sundaychallenge.dto.RosterImportRequest;
import com.sundaychallenge.dto.RosterImportResponse;
import com.sundaychallenge.dto.StudentIntelligenceStatsResponse;
import com.sundaychallenge.dto.StudentRosterResponse;
import com.sundaychallenge.entity.AdminAccess;
import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.Question;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.repository.AdminAccessRepository;
import com.sundaychallenge.repository.AttemptAnswerRepository;
import com.sundaychallenge.repository.AttemptRepository;
import com.sundaychallenge.repository.ChallengeQuestionRepository;
import com.sundaychallenge.repository.ChallengeRepository;
import com.sundaychallenge.repository.QuestionRepository;
import com.sundaychallenge.repository.StudentRosterRepository;
import com.sundaychallenge.repository.UserRepository;
import com.sundaychallenge.service.AdminAccessService;
import com.sundaychallenge.service.AdminService;
import com.sundaychallenge.service.AuthService;
import com.sundaychallenge.service.ChallengeService;
import com.sundaychallenge.service.CustomOAuth2UserService;
import com.sundaychallenge.service.StudentRosterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StudentIntelligenceAndAdminControlTest {

    @Autowired
    private AdminAccessService adminAccessService;

    @Autowired
    private StudentRosterService studentRosterService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminAccessRepository adminAccessRepository;

    @Autowired
    private StudentRosterRepository studentRosterRepository;

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
    private AdminStudentController adminStudentController;

    @Autowired
    private AdminAccessController adminAccessController;

    @MockBean
    private AuthService authService;

    @MockBean
    private OAuth2User mockOAuth2User;

    private User primaryAdminUser;
    private User student1;
    private User student2;

    @BeforeEach
    void setUp() {
        attemptAnswerRepository.deleteAll();
        attemptRepository.deleteAll();
        challengeQuestionRepository.deleteAll();
        questionRepository.deleteAll();
        challengeRepository.deleteAll();
        studentRosterRepository.deleteAll();
        adminAccessRepository.deleteAll();

        // Safely manage Primary Admin User
        primaryAdminUser = userRepository.findByEmail("244g1a05cp@srit.ac.in").orElseGet(() -> {
            User newAdmin = new User("google-admin-1", "Primary Admin", "244g1a05cp@srit.ac.in", "pic", Role.ADMIN);
            newAdmin.setUsername("244G1A05CP");
            return userRepository.save(newAdmin);
        });

        // Clean up other non-primary users
        userRepository.findAll().forEach(u -> {
            if (!"244g1a05cp@srit.ac.in".equalsIgnoreCase(u.getEmail())) {
                userRepository.delete(u);
            }
        });

        // Seed Primary Admin Access
        adminAccessService.initPrimaryAdmin();

        // Seed Student 1 (Participated)
        student1 = new User("google-stu-1", "Ravi Kumar", "ravi@example.com", "pic", Role.STUDENT);
        student1.setUsername("21A01A0501");
        student1 = userRepository.save(student1);

        // Seed Student 2 (Never Participated)
        student2 = new User("google-stu-2", "Anitha Reddy", "anitha@example.com", "pic", Role.STUDENT);
        student2.setUsername("21A01A0503");
        student2 = userRepository.save(student2);

        when(authService.getAuthenticatedUser(any())).thenReturn(primaryAdminUser);
    }

    @Test
    void test01_StudentIntelligenceStatsCalculatedCorrectly() {
        // Import roster containing 3 students
        String csv = "rollNumber,name,email\n21A01A0501,Ravi Kumar,ravi@example.com\n21A01A0502,Suresh Kumar,suresh@example.com\n21A01A0503,Anitha Reddy,anitha@example.com";
        studentRosterService.importRosterCsv(csv);

        // Create challenge & student1 attempt
        Challenge challenge = challengeRepository.save(new Challenge("Math Quiz", "Desc", Category.REASONING, Difficulty.EASY, 15, 0, 0, true));
        Question q1 = questionRepository.save(new Question("2+2?", "3", "4", "5", "6", "B", 10, "Exp"));
        challengeService.startChallenge(challenge.getId(), student1);

        ResponseEntity<StudentIntelligenceStatsResponse> statsResp = adminStudentController.getStudentIntelligenceStats(mockOAuth2User);
        assertEquals(HttpStatus.OK, statsResp.getStatusCode());
        StudentIntelligenceStatsResponse stats = statsResp.getBody();

        assertNotNull(stats);
        assertEquals(3, stats.totalCollegeRoster());
        assertEquals(2, stats.registeredStudents());
        assertEquals(1, stats.notRegisteredStudents()); // 21A01A0502 is in roster but not registered
        assertEquals(1, stats.studentsParticipated()); // student1
        assertEquals(1, stats.studentsNeverParticipated()); // student2
        assertEquals(1, stats.totalAttempts());
    }

    @Test
    void test02_NeverParticipatedStudentsFilteredCorrectly() {
        Challenge challenge = challengeRepository.save(new Challenge("Logic Quiz", "Desc", Category.REASONING, Difficulty.EASY, 15, 0, 0, true));
        challengeService.startChallenge(challenge.getId(), student1);

        ResponseEntity<List<com.sundaychallenge.dto.AdminStudentResponse>> neverPart = adminStudentController.getNeverParticipatedStudents(mockOAuth2User);
        assertEquals(HttpStatus.OK, neverPart.getStatusCode());
        List<com.sundaychallenge.dto.AdminStudentResponse> list = neverPart.getBody();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("21A01A0503", list.get(0).username());
    }

    @Test
    void test03_CollegeRosterCsvImportAndDuplicateHandling() {
        String csv = "rollNumber,name,email\n21A01A0501,Ravi Kumar,ravi@example.com\n21A01A0502,Suresh Kumar,suresh@example.com";
        ResponseEntity<RosterImportResponse> resp1 = adminStudentController.importStudentRoster(new RosterImportRequest(csv), mockOAuth2User);

        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertEquals(2, resp1.getBody().importedCount());
        assertEquals(0, resp1.getBody().duplicateCount());

        // Re-import same CSV -> should count as duplicates and skip
        ResponseEntity<RosterImportResponse> resp2 = adminStudentController.importStudentRoster(new RosterImportRequest(csv), mockOAuth2User);
        assertEquals(2, resp2.getBody().duplicateCount());
        assertEquals(0, resp2.getBody().importedCount());
    }

    @Test
    void test04_NotRegisteredComparison() {
        String csv = "rollNumber,name,email\n21A01A0501,Ravi Kumar,ravi@example.com\n21A01A0502,Suresh Kumar,suresh@example.com";
        studentRosterService.importRosterCsv(csv);

        ResponseEntity<List<NotRegisteredStudentResponse>> notRegResp = adminStudentController.getNotRegisteredStudents(mockOAuth2User);
        assertEquals(HttpStatus.OK, notRegResp.getStatusCode());
        List<NotRegisteredStudentResponse> list = notRegResp.getBody();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("21A01A0502", list.get(0).rollNumber());
    }

    @Test
    void test05_CsvExport() {
        ResponseEntity<byte[]> exportResp = adminStudentController.exportStudentsCsv(null, null, null, mockOAuth2User);
        assertEquals(HttpStatus.OK, exportResp.getStatusCode());
        String csvContent = new String(exportResp.getBody());

        assertTrue(csvContent.contains("Roll Number,Name,Email"));
        assertTrue(csvContent.contains("21A01A0501"));
        assertTrue(csvContent.contains("21A01A0503"));
    }

    @Test
    void test06_PrimaryAdminSeededAndProtected() {
        ResponseEntity<List<AdminAccessResponse>> admins = adminAccessController.getAllAdmins(mockOAuth2User);
        assertEquals(HttpStatus.OK, admins.getStatusCode());

        AdminAccessResponse primary = admins.getBody().stream()
                .filter(AdminAccessResponse::primaryAdmin)
                .findFirst()
                .orElse(null);

        assertNotNull(primary);
        assertEquals("244g1a05cp@srit.ac.in", primary.email());

        // Attempting to remove primary admin must throw BAD_REQUEST
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> adminAccessController.removeAdmin(primary.id(), mockOAuth2User));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Primary Admin"));
    }

    @Test
    void test07_AddAdminEmailGrantsAdminRole() {
        // Create student account for secondary admin prior to granting admin access
        User newAdminUser = userRepository.save(new User("g-sec", "Secondary Admin", "secondary@srit.ac.in", "pic", Role.STUDENT));

        AdminAccessRequest request = new AdminAccessRequest("secondary@srit.ac.in", "Secondary Admin");
        ResponseEntity<AdminAccessResponse> created = adminAccessController.addAdmin(request, mockOAuth2User);

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals("secondary@srit.ac.in", created.getBody().email());

        // Confirm existing User account role is updated to ADMIN
        User updatedUser = userRepository.findByEmail("secondary@srit.ac.in").orElseThrow();
        assertEquals(Role.ADMIN, updatedUser.getRole());
    }

    @Test
    void test08_RemoveAdminRevokesAccessWithoutDeletingUser() {
        // Add secondary admin
        User secUser = userRepository.save(new User("g-sec", "Secondary Admin", "secondary@srit.ac.in", "pic", Role.STUDENT));
        AdminAccessResponse secAdmin = adminAccessService.addAdmin(new AdminAccessRequest("secondary@srit.ac.in", "Sec Admin"), primaryAdminUser);

        // Remove secondary admin access
        adminAccessService.removeAdmin(secAdmin.id(), primaryAdminUser);

        // Confirm user account exists but role is demoted to STUDENT
        User demotedUser = userRepository.findByEmail("secondary@srit.ac.in").orElseThrow();
        assertNotNull(demotedUser);
        assertEquals(Role.STUDENT, demotedUser.getRole());
    }

    @Test
    void test09_DuplicateAdminEmailRejected() {
        AdminAccessRequest request = new AdminAccessRequest("secondary@srit.ac.in", "Secondary Admin");
        adminAccessController.addAdmin(request, mockOAuth2User);

        // Attempting to add duplicate email -> 409 CONFLICT
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> adminAccessController.addAdmin(request, mockOAuth2User));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void test10_StudentCannotAccessAdminEndpoints() {
        when(authService.getAuthenticatedUser(any())).thenReturn(student1);

        assertThrows(ResponseStatusException.class, () -> adminStudentController.getStudentIntelligenceStats(mockOAuth2User));
        assertThrows(ResponseStatusException.class, () -> adminAccessController.getAllAdmins(mockOAuth2User));
    }

    @Test
    void test11_CsvImportFlexibleColumnsAndHeaderAndBlankLinesHandling() {
        String csv = """
                rollNumber,name,email
                244G1A05CS,"Srinivasulu K",srinivasulu3621@gmail.com
                
                # Comment line
                244G1A06CS,Ravi Kumar
                244G1A07CS
                ,Missing Roll Number
                """;

        RosterImportResponse res = studentRosterService.importRosterCsv(csv);

        assertNotNull(res);
        assertEquals(4, res.totalRows());
        assertEquals(3, res.importedRows()); // 244G1A05CS, 244G1A06CS, 244G1A07CS
        assertEquals(1, res.invalidRows()); // Missing roll number row
        assertEquals(0, res.duplicateRows());
        assertTrue(res.invalidDetails().size() >= 1);

        // Assert roll numbers saved properly in roster
        assertTrue(studentRosterRepository.existsByRollNumberIgnoreCase("244G1A05CS"));
        assertTrue(studentRosterRepository.existsByRollNumberIgnoreCase("244G1A06CS"));
        assertTrue(studentRosterRepository.existsByRollNumberIgnoreCase("244G1A07CS"));
    }

    @Test
    void test12_ReImportingSameCsvIsSafeAndNoDuplicatesCreated() {
        String csv = "rollNumber,name,email\n244G1A05CS,Srinivasulu K,srinivasulu3621@gmail.com\n244G1A06CS,Ravi Kumar,ravi@gmail.com";

        RosterImportResponse res1 = studentRosterService.importRosterCsv(csv);
        assertEquals(2, res1.importedRows());

        // Second import of identical CSV
        RosterImportResponse res2 = studentRosterService.importRosterCsv(csv);
        assertEquals(0, res2.importedRows());
        assertEquals(2, res2.duplicateRows());

        // Total count in database should remain 2
        assertEquals(2, studentRosterRepository.count());
    }
}
