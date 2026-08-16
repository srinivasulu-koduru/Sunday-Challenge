package com.sundaychallenge.service;

import com.sundaychallenge.dto.AdminAccessRequest;
import com.sundaychallenge.dto.AdminAccessResponse;
import com.sundaychallenge.entity.AdminAccess;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.repository.AdminAccessRepository;
import com.sundaychallenge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAccessService {

    private static final Logger log = LoggerFactory.getLogger(AdminAccessService.class);
    private static final String PRIMARY_ADMIN_EMAIL = "244g1a05cp@srit.ac.in";

    private final AdminAccessRepository adminAccessRepository;
    private final UserRepository userRepository;
    private final Set<String> propertyAdminEmails;

    public AdminAccessService(AdminAccessRepository adminAccessRepository,
                              UserRepository userRepository,
                              @Value("${app.admin.emails:244g1a05cp@srit.ac.in}") String adminEmailsConfig) {
        this.adminAccessRepository = adminAccessRepository;
        this.userRepository = userRepository;
        this.propertyAdminEmails = Arrays.stream(adminEmailsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initPrimaryAdmin() {
        // Always ensure primary admin email is seeded and active
        Optional<AdminAccess> primaryOpt = adminAccessRepository.findByEmailIgnoreCase(PRIMARY_ADMIN_EMAIL);
        if (primaryOpt.isEmpty()) {
            AdminAccess primary = new AdminAccess(PRIMARY_ADMIN_EMAIL, "Primary Admin", true, true);
            adminAccessRepository.save(primary);
            log.info("[ADMIN CONTROL] Seeded Primary Admin access for {}", PRIMARY_ADMIN_EMAIL);
        } else {
            AdminAccess primary = primaryOpt.get();
            if (!primary.isActive() || !primary.isPrimaryAdmin()) {
                primary.setActive(true);
                primary.setPrimaryAdmin(true);
                adminAccessRepository.save(primary);
                log.info("[ADMIN CONTROL] Restored Primary Admin access state for {}", PRIMARY_ADMIN_EMAIL);
            }
        }

        // Also ensure user record in UserRepository has Role.ADMIN if present
        userRepository.findByEmail(PRIMARY_ADMIN_EMAIL).ifPresent(user -> {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
                log.info("[ADMIN CONTROL] Updated Primary Admin user entity role to ADMIN for {}", PRIMARY_ADMIN_EMAIL);
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isEmailAdmin(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();

        if (PRIMARY_ADMIN_EMAIL.equalsIgnoreCase(cleanEmail) || propertyAdminEmails.contains(cleanEmail)) {
            return true;
        }

        return adminAccessRepository.existsByEmailIgnoreCaseAndActiveTrue(cleanEmail);
    }

    @Transactional(readOnly = true)
    public List<AdminAccessResponse> getAllAdmins() {
        return adminAccessRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(AdminAccess::isActive)
                .map(AdminAccessResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AdminAccessResponse addAdmin(AdminAccessRequest request, User currentUser) {
        if (request == null || request.email() == null || request.email().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin email address is required");
        }

        String email = request.email().trim().toLowerCase();
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address format");
        }

        Optional<AdminAccess> existingOpt = adminAccessRepository.findByEmailIgnoreCase(email);
        AdminAccess adminAccess;

        if (existingOpt.isPresent()) {
            adminAccess = existingOpt.get();
            if (adminAccess.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin access for " + email + " already exists and is active");
            }
            adminAccess.setActive(true);
            if (request.name() != null && !request.name().trim().isEmpty()) {
                adminAccess.setName(request.name().trim());
            }
        } else {
            boolean isPrimary = PRIMARY_ADMIN_EMAIL.equalsIgnoreCase(email);
            adminAccess = new AdminAccess(email, request.name() != null ? request.name().trim() : "Administrator", true, isPrimary);
        }

        adminAccess = adminAccessRepository.save(adminAccess);

        // Update corresponding User record if present
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRole(Role.ADMIN);
            userRepository.save(user);
            log.info("[ADMIN CONTROL] Promoted user account {} to ADMIN role.", email);
        });

        log.info("[ADMIN CONTROL] Added admin access for email: {} by {}", email, currentUser != null ? currentUser.getEmail() : "SYSTEM");
        return AdminAccessResponse.fromEntity(adminAccess);
    }

    @Transactional
    public void removeAdmin(Long id, User currentUser) {
        AdminAccess adminAccess = adminAccessRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin access record not found"));

        if (adminAccess.isPrimaryAdmin() || PRIMARY_ADMIN_EMAIL.equalsIgnoreCase(adminAccess.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary Admin (244g1a05cp@srit.ac.in) access cannot be removed");
        }

        if (currentUser != null && currentUser.getEmail() != null && currentUser.getEmail().equalsIgnoreCase(adminAccess.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot revoke your own admin access privileges");
        }

        long activeCount = adminAccessRepository.countByActiveTrue();
        if (activeCount <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the final remaining active administrator");
        }

        adminAccess.setActive(false);
        adminAccessRepository.save(adminAccess);

        // Demote existing User record if present (without deleting account)
        userRepository.findByEmail(adminAccess.getEmail()).ifPresent(user -> {
            if (!PRIMARY_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail())) {
                user.setRole(Role.STUDENT);
                userRepository.save(user);
                log.info("[ADMIN CONTROL] Demoted user account {} from ADMIN to STUDENT role.", user.getEmail());
            }
        });

        log.info("[ADMIN CONTROL] Removed admin access for ID: {}, Email: {} by {}", id, adminAccess.getEmail(), currentUser != null ? currentUser.getEmail() : "SYSTEM");
    }
}
