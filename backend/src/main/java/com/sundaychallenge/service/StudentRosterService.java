package com.sundaychallenge.service;

import com.sundaychallenge.dto.NotRegisteredStudentResponse;
import com.sundaychallenge.dto.RosterImportResponse;
import com.sundaychallenge.dto.StudentRosterResponse;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.StudentRoster;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.StudentRosterRepository;
import com.sundaychallenge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing official college student rosters, parsing CSV imports,
 * and performing comparison against registered portal users.
 */
@Service
public class StudentRosterService {

    private static final Logger log = LoggerFactory.getLogger(StudentRosterService.class);

    private final StudentRosterRepository studentRosterRepository;
    private final UserRepository userRepository;

    public StudentRosterService(StudentRosterRepository studentRosterRepository, UserRepository userRepository) {
        this.studentRosterRepository = studentRosterRepository;
        this.userRepository = userRepository;
    }

    private String cleanFieldValue(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() >= 2) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }

    @Transactional
    public RosterImportResponse importRosterCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            List<String> emptyErr = List.of("CSV content is empty");
            return new RosterImportResponse(0, 0, 0, 0, 0, 0, emptyErr, emptyErr);
        }

        String[] lines = csvContent.split("\\r?\\n");
        int totalRows = 0;
        int importedRows = 0;
        int duplicateRows = 0;
        int invalidRows = 0;
        List<String> invalidDetails = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        Set<String> batchRollNumbers = new HashSet<>();
        Set<String> batchEmails = new HashSet<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Skip header line if detected
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("rollnumber") || lowerLine.contains("roll_number") || lowerLine.contains("roll no")) {
                continue;
            }

            totalRows++;
            String[] parts = line.split(",");
            if (parts.length == 0) {
                invalidRows++;
                String err = "Row " + (i + 1) + ": Empty row content.";
                invalidDetails.add(err);
                messages.add(err);
                continue;
            }

            String rollNumber = cleanFieldValue(parts[0]).toUpperCase();
            if (rollNumber.isEmpty()) {
                invalidRows++;
                String err = "Row " + (i + 1) + ": Missing roll number.";
                invalidDetails.add(err);
                messages.add(err);
                continue;
            }

            String name = (parts.length > 1) ? cleanFieldValue(parts[1]) : "";
            if (name.isEmpty()) {
                name = rollNumber; // Fallback to roll number if name omitted
            }

            String email = (parts.length > 2) ? cleanFieldValue(parts[2]).toLowerCase() : null;
            if (email != null && email.trim().isEmpty()) {
                email = null;
            }

            // Check duplicates in batch pass
            if (batchRollNumbers.contains(rollNumber) || (email != null && batchEmails.contains(email))) {
                duplicateRows++;
                String msg = "Row " + (i + 1) + ": Duplicate entry for roll number (" + rollNumber + ") in CSV batch.";
                messages.add(msg);
                continue;
            }

            // Check duplicates in database
            boolean rollExists = studentRosterRepository.existsByRollNumberIgnoreCase(rollNumber);
            boolean emailExists = email != null && studentRosterRepository.existsByEmailIgnoreCase(email);

            if (rollExists || emailExists) {
                duplicateRows++;
                String msg = "Row " + (i + 1) + ": Student (" + rollNumber + (email == null ? "" : " / " + email) + ") already exists in college roster.";
                messages.add(msg);
                continue;
            }

            // Save to database
            StudentRoster roster = new StudentRoster(rollNumber, name, email);
            studentRosterRepository.save(roster);

            batchRollNumbers.add(rollNumber);
            if (email != null) batchEmails.add(email);

            importedRows++;
        }

        // Calculate registered vs. not registered counts for response summary
        List<StudentRoster> fullRoster = studentRosterRepository.findAll();
        List<User> students = userRepository.findByRole(Role.STUDENT);

        Set<String> registeredRolls = students.stream()
                .map(User::getUsername)
                .filter(u -> u != null && !u.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Set<String> registeredEmails = students.stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        int registeredCount = 0;
        int notRegisteredCount = 0;

        for (StudentRoster r : fullRoster) {
            boolean isReg = registeredRolls.contains(r.getRollNumber().toUpperCase()) ||
                    (r.getEmail() != null && !r.getEmail().isEmpty() && registeredEmails.contains(r.getEmail().toLowerCase()));
            if (isReg) {
                registeredCount++;
            } else {
                notRegisteredCount++;
            }
        }

        log.info("[ROSTER IMPORT] Processed: {}, Imported: {}, Duplicates: {}, Invalid: {}, Registered: {}, Unregistered: {}",
                totalRows, importedRows, duplicateRows, invalidRows, registeredCount, notRegisteredCount);

        return new RosterImportResponse(
                totalRows,
                importedRows,
                duplicateRows,
                invalidRows,
                registeredCount,
                notRegisteredCount,
                invalidDetails,
                messages
        );
    }

    @Transactional(readOnly = true)
    public List<StudentRosterResponse> getFullRoster() {
        List<StudentRoster> rosterList = studentRosterRepository.findAllByOrderByRollNumberAsc();
        List<User> students = userRepository.findByRole(Role.STUDENT);

        Set<String> registeredRolls = students.stream()
                .map(User::getUsername)
                .filter(u -> u != null && !u.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Set<String> registeredEmails = students.stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return rosterList.stream().map(r -> {
            boolean registered = registeredRolls.contains(r.getRollNumber().toUpperCase()) ||
                    (r.getEmail() != null && !r.getEmail().isEmpty() && registeredEmails.contains(r.getEmail().toLowerCase()));
            return new StudentRosterResponse(r.getId(), r.getRollNumber(), r.getName(), r.getEmail(), registered, r.getCreatedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<NotRegisteredStudentResponse> getNotRegisteredStudents() {
        List<StudentRoster> rosterList = studentRosterRepository.findAllByOrderByRollNumberAsc();
        List<User> students = userRepository.findByRole(Role.STUDENT);

        Set<String> registeredRolls = students.stream()
                .map(User::getUsername)
                .filter(u -> u != null && !u.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        Set<String> registeredEmails = students.stream()
                .map(User::getEmail)
                .filter(e -> e != null && !e.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return rosterList.stream()
                .filter(r -> !registeredRolls.contains(r.getRollNumber().toUpperCase()) &&
                        (r.getEmail() == null || r.getEmail().isEmpty() || !registeredEmails.contains(r.getEmail().toLowerCase())))
                .map(r -> new NotRegisteredStudentResponse(r.getId(), r.getRollNumber(), r.getName(), r.getEmail(), "NOT_REGISTERED", r.getCreatedAt()))
                .toList();
    }
}
