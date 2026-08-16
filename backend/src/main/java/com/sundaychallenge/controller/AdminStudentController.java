package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AdminStudentDetailsResponse;
import com.sundaychallenge.dto.AdminStudentResponse;
import com.sundaychallenge.dto.NotRegisteredStudentResponse;
import com.sundaychallenge.dto.RosterImportRequest;
import com.sundaychallenge.dto.RosterImportResponse;
import com.sundaychallenge.dto.StudentIntelligenceStatsResponse;
import com.sundaychallenge.dto.StudentRosterResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.service.AdminService;
import com.sundaychallenge.service.AuthService;
import com.sundaychallenge.service.StudentRosterService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controller for T&P Student Intelligence, roster comparison, filtering, CSV exports,
 * and student profile inspection. Restricted exclusively to users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentController {

    private final AdminService adminService;
    private final StudentRosterService studentRosterService;
    private final AuthService authService;

    public AdminStudentController(AdminService adminService,
                                  StudentRosterService studentRosterService,
                                  AuthService authService) {
        this.adminService = adminService;
        this.studentRosterService = studentRosterService;
        this.authService = authService;
    }

    private void verifyAdmin(OAuth2User oauth2User) {
        try {
            User user = authService.getAuthenticatedUser(oauth2User);
            if (user.getRole() != Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Admin authorization required");
            }
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
    }

    @GetMapping("/students/intelligence-stats")
    public ResponseEntity<StudentIntelligenceStatsResponse> getStudentIntelligenceStats(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getStudentIntelligenceStats());
    }

    @GetMapping("/students")
    public ResponseEntity<List<AdminStudentResponse>> getFilteredStudents(@RequestParam(value = "query", required = false) String query,
                                                                          @RequestParam(value = "participation", required = false) String participation,
                                                                          @RequestParam(value = "performance", required = false) String performance,
                                                                          @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getFilteredStudents(query, participation, performance));
    }

    public ResponseEntity<List<AdminStudentResponse>> getAllStudents(String query, OAuth2User oauth2User) {
        return getFilteredStudents(query, null, null, oauth2User);
    }

    @GetMapping("/students/never-participated")
    public ResponseEntity<List<AdminStudentResponse>> getNeverParticipatedStudents(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getNeverParticipatedStudents());
    }

    @GetMapping("/students/not-registered")
    public ResponseEntity<List<NotRegisteredStudentResponse>> getNotRegisteredStudents(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(studentRosterService.getNotRegisteredStudents());
    }

    @GetMapping("/student-roster")
    public ResponseEntity<List<StudentRosterResponse>> getFullStudentRoster(@AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(studentRosterService.getFullRoster());
    }

    @PostMapping({"/student-roster/import", "/students/roster/import"})
    public ResponseEntity<RosterImportResponse> importStudentRoster(@RequestBody RosterImportRequest request,
                                                                    @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        RosterImportResponse response = studentRosterService.importRosterCsv(request != null ? request.csvContent() : "");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students/export")
    public ResponseEntity<byte[]> exportStudentsCsv(@RequestParam(value = "query", required = false) String query,
                                                     @RequestParam(value = "participation", required = false) String participation,
                                                     @RequestParam(value = "performance", required = false) String performance,
                                                     @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        byte[] csvBytes = adminService.exportStudentsCsv(query, participation, performance);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tnp_student_intelligence.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<AdminStudentDetailsResponse> getStudentDetails(@PathVariable("id") Long id,
                                                                          @AuthenticationPrincipal OAuth2User oauth2User) {
        verifyAdmin(oauth2User);
        return ResponseEntity.ok(adminService.getStudentDetails(id));
    }
}
