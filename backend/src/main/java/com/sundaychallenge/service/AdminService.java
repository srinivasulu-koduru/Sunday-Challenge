package com.sundaychallenge.service;

import com.sundaychallenge.dto.AdminAttemptDetailsResponse;
import com.sundaychallenge.dto.AdminAttemptResponse;
import com.sundaychallenge.dto.AdminChallengeDetailsResponse;
import com.sundaychallenge.dto.AdminChallengeRequest;
import com.sundaychallenge.dto.AdminChallengeResponse;
import com.sundaychallenge.dto.AdminQuestionRequest;
import com.sundaychallenge.dto.AdminQuestionResponse;
import com.sundaychallenge.dto.AdminStatsResponse;
import com.sundaychallenge.dto.AdminStudentDetailsResponse;
import com.sundaychallenge.dto.AdminStudentResponse;
import com.sundaychallenge.dto.AnswerReviewResponse;
import com.sundaychallenge.dto.CategoryReportResponse;
import com.sundaychallenge.dto.ChallengeReportResponse;
import com.sundaychallenge.dto.DifficultyReportResponse;
import com.sundaychallenge.dto.LeaderboardEntryResponse;
import com.sundaychallenge.dto.SaveChallengeWithQuestionsRequest;
import com.sundaychallenge.dto.UserAttemptSummaryResponse;
import com.sundaychallenge.entity.Attempt;
import com.sundaychallenge.entity.Challenge;
import com.sundaychallenge.entity.ChallengeQuestion;
import com.sundaychallenge.entity.Question;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.ChallengeStatus;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.repository.AttemptAnswerRepository;
import com.sundaychallenge.repository.AttemptRepository;
import com.sundaychallenge.repository.ChallengeQuestionRepository;
import com.sundaychallenge.repository.ChallengeRepository;
import com.sundaychallenge.repository.QuestionRepository;
import com.sundaychallenge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service handling administrative operations, platform management, challenge scheduling,
 * question configuration, safety checks, leaderboard generation, and analytical reporting.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final QuestionRepository questionRepository;
    private final ChallengeQuestionRepository challengeQuestionRepository;
    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final ChallengeService challengeService;

    public AdminService(UserRepository userRepository,
                        ChallengeRepository challengeRepository,
                        QuestionRepository questionRepository,
                        ChallengeQuestionRepository challengeQuestionRepository,
                        AttemptRepository attemptRepository,
                        AttemptAnswerRepository attemptAnswerRepository,
                        ChallengeService challengeService) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.questionRepository = questionRepository;
        this.challengeQuestionRepository = challengeQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.challengeService = challengeService;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalChallenges = challengeRepository.count();
        long activeChallenges = challengeRepository.findByActiveTrue().size();
        long totalQuestions = questionRepository.count();
        long totalAttempts = attemptRepository.count();
        long completedAttempts = attemptRepository.countByStatus(AttemptStatus.COMPLETED);
        long inProgressAttempts = attemptRepository.countByStatus(AttemptStatus.IN_PROGRESS);
        long expiredAttempts = attemptRepository.countByStatus(AttemptStatus.EXPIRED);

        List<Attempt> allAttempts = attemptRepository.findAll();
        int totalPointsEarned = allAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                .mapToInt(Attempt::getPointsEarned)
                .sum();

        double averageScore = 0.0;
        List<Attempt> finishedAttempts = allAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                .toList();

        if (!finishedAttempts.isEmpty()) {
            double totalPct = finishedAttempts.stream()
                    .mapToDouble(a -> a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0)
                    .sum();
            averageScore = Math.round((totalPct / finishedAttempts.size()) * 100.0) / 100.0;
        }

        double completionRate = 0.0;
        if (totalAttempts > 0) {
            completionRate = Math.round(((double) completedAttempts / totalAttempts * 100.0) * 100.0) / 100.0;
        }

        return new AdminStatsResponse(
                totalStudents,
                totalChallenges,
                activeChallenges,
                totalQuestions,
                totalAttempts,
                completedAttempts,
                inProgressAttempts,
                expiredAttempts,
                totalPointsEarned,
                averageScore,
                completionRate
        );
    }

    // =========================================================================
    // CHALLENGE MANAGEMENT & SCHEDULING
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AdminChallengeResponse> getAllChallenges() {
        return challengeRepository.findAll().stream()
                .map(AdminChallengeResponse::fromEntity)
                .sorted(Comparator.comparing(AdminChallengeResponse::id).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminChallengeDetailsResponse getChallengeDetailsWithQuestions(Long id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));

        List<ChallengeQuestion> cqList = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(id);
        List<AdminQuestionResponse> questions = cqList.stream()
                .map(cq -> AdminQuestionResponse.fromEntity(cq.getQuestion(), challenge.getId(), challenge.getTitle()))
                .toList();

        return new AdminChallengeDetailsResponse(AdminChallengeResponse.fromEntity(challenge), questions);
    }

    @Transactional
    public AdminChallengeResponse createChallenge(AdminChallengeRequest request) {
        validateChallengeRequest(request);

        boolean active = request.active() != null ? request.active() : true;
        ChallengeStatus status = request.status() != null ? request.status() : (active ? ChallengeStatus.ACTIVE : ChallengeStatus.INACTIVE);

        Challenge challenge = new Challenge(
                request.title().trim(),
                request.description() != null ? request.description().trim() : "",
                request.category(),
                request.difficulty(),
                request.durationMinutes(),
                0,
                0,
                active,
                request.startTime(),
                request.endTime(),
                status
        );

        challenge = challengeRepository.save(challenge);
        log.info("[ADMIN] Created challenge ID: {}, Title: {}, Schedule: {} to {}", challenge.getId(), challenge.getTitle(), challenge.getStartTime(), challenge.getEndTime());
        return AdminChallengeResponse.fromEntity(challenge);
    }

    @Transactional
    public AdminChallengeResponse updateChallenge(Long id, AdminChallengeRequest request) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));

        if (request.title() != null && !request.title().trim().isEmpty()) {
            challenge.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            challenge.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            challenge.setCategory(request.category());
        }
        if (request.difficulty() != null) {
            challenge.setDifficulty(request.difficulty());
        }
        if (request.durationMinutes() != null && request.durationMinutes() > 0) {
            challenge.setDurationMinutes(request.durationMinutes());
        }
        if (request.active() != null) {
            challenge.setActive(request.active());
        }
        if (request.startTime() != null) {
            challenge.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            challenge.setEndTime(request.endTime());
        }
        if (request.status() != null) {
            challenge.setStatus(request.status());
        }

        challenge = challengeRepository.save(challenge);
        log.info("[ADMIN] Updated challenge ID: {}", id);
        return AdminChallengeResponse.fromEntity(challenge);
    }

    @Transactional
    public AdminChallengeDetailsResponse createChallengeWithQuestions(SaveChallengeWithQuestionsRequest request) {
        if (request == null || request.challenge() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge configuration is required");
        }

        AdminChallengeResponse challengeResp = createChallenge(request.challenge());
        Long challengeId = challengeResp.id();

        if (request.questions() != null && !request.questions().isEmpty()) {
            int order = 1;
            for (AdminQuestionRequest qReq : request.questions()) {
                addQuestionToChallenge(challengeId, qReq, order++);
            }
        }

        return getChallengeDetailsWithQuestions(challengeId);
    }

    @Transactional
    public AdminChallengeDetailsResponse updateChallengeWithQuestions(Long id, SaveChallengeWithQuestionsRequest request) {
        if (request == null || request.challenge() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge configuration is required");
        }

        updateChallenge(id, request.challenge());

        if (request.questions() != null) {
            // Remove old links and save updated question list
            List<ChallengeQuestion> existingCq = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(id);
            challengeQuestionRepository.deleteAll(existingCq);

            int order = 1;
            for (AdminQuestionRequest qReq : request.questions()) {
                addQuestionToChallenge(id, qReq, order++);
            }
        }

        return getChallengeDetailsWithQuestions(id);
    }

    @Transactional
    public AdminChallengeResponse toggleChallengeStatus(Long id, boolean active) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));
        challenge.setActive(active);
        if (!active) {
            challenge.setStatus(ChallengeStatus.INACTIVE);
        } else {
            challenge.setStatus(ChallengeStatus.ACTIVE);
        }
        challenge = challengeRepository.save(challenge);
        log.info("[ADMIN] Toggled active status of challenge ID: {} to {}", id, active);
        return AdminChallengeResponse.fromEntity(challenge);
    }

    @Transactional
    public void deleteChallenge(Long id) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));

        if (attemptRepository.existsByChallengeId(id)) {
            log.warn("[ADMIN] Rejecting deletion of challenge ID: {} because student attempts exist.", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This challenge cannot be deleted because students have already attempted it. Deactivate it instead.");
        }

        List<ChallengeQuestion> cqList = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(id);
        challengeQuestionRepository.deleteAll(cqList);
        challengeRepository.delete(challenge);
        log.info("[ADMIN] Safely deleted challenge ID: {}", id);
    }

    // =========================================================================
    // EMBEDDED QUESTION MANAGEMENT
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AdminQuestionResponse> getQuestionsForChallenge(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));

        List<ChallengeQuestion> cqList = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(challengeId);
        return cqList.stream()
                .map(cq -> AdminQuestionResponse.fromEntity(cq.getQuestion(), challenge.getId(), challenge.getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminQuestionResponse> getAllQuestions() {
        List<Question> questions = questionRepository.findAll();
        List<AdminQuestionResponse> list = new ArrayList<>();

        for (Question q : questions) {
            Optional<ChallengeQuestion> cqOpt = challengeQuestionRepository.findAll().stream()
                    .filter(cq -> cq.getQuestion().getId().equals(q.getId()))
                    .findFirst();
            Long challengeId = cqOpt.map(cq -> cq.getChallenge().getId()).orElse(null);
            String challengeTitle = cqOpt.map(cq -> cq.getChallenge().getTitle()).orElse(null);
            list.add(AdminQuestionResponse.fromEntity(q, challengeId, challengeTitle));
        }

        return list;
    }

    @Transactional
    public AdminQuestionResponse createQuestion(AdminQuestionRequest request) {
        if (request.challengeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge ID is required for adding questions");
        }
        int nextOrder = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(request.challengeId()).size() + 1;
        return addQuestionToChallenge(request.challengeId(), request, nextOrder);
    }

    @Transactional
    public AdminQuestionResponse addQuestionToChallenge(Long challengeId, AdminQuestionRequest request, Integer questionOrder) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found"));

        validateQuestionRequest(request);

        Question question = new Question(
                request.questionText().trim(),
                request.optionA().trim(),
                request.optionB().trim(),
                request.optionC().trim(),
                request.optionD().trim(),
                request.correctOption().trim().toUpperCase(),
                request.points() != null ? request.points() : 10,
                request.explanation() != null ? request.explanation().trim() : ""
        );
        question = questionRepository.save(question);

        int order = questionOrder != null ? questionOrder : (challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(challengeId).size() + 1);
        challengeQuestionRepository.save(new ChallengeQuestion(challenge, question, order));

        recalculateChallengeTotals(challengeId);
        log.info("[ADMIN] Added question ID: {} to challenge ID: {}", question.getId(), challengeId);
        return AdminQuestionResponse.fromEntity(question, challenge.getId(), challenge.getTitle());
    }

    @Transactional
    public AdminQuestionResponse updateQuestion(Long id, AdminQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        validateQuestionRequest(request);

        question.setQuestionText(request.questionText().trim());
        question.setOptionA(request.optionA().trim());
        question.setOptionB(request.optionB().trim());
        question.setOptionC(request.optionC().trim());
        question.setOptionD(request.optionD().trim());
        question.setCorrectOption(request.correctOption().trim().toUpperCase());
        question.setPoints(request.points() != null ? request.points() : 10);
        question.setExplanation(request.explanation() != null ? request.explanation().trim() : "");

        question = questionRepository.save(question);

        Long challengeId = null;
        String challengeTitle = null;
        Optional<ChallengeQuestion> cqOpt = challengeQuestionRepository.findAll().stream()
                .filter(cq -> cq.getQuestion().getId().equals(id))
                .findFirst();

        if (cqOpt.isPresent()) {
            challengeId = cqOpt.get().getChallenge().getId();
            challengeTitle = cqOpt.get().getChallenge().getTitle();
            recalculateChallengeTotals(challengeId);
        }

        log.info("[ADMIN] Updated question ID: {}", id);
        return AdminQuestionResponse.fromEntity(question, challengeId, challengeTitle);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        if (attemptAnswerRepository.existsByQuestionId(id)) {
            log.warn("[ADMIN] Rejecting deletion of question ID: {} because student attempt answers reference it.", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete question because it is referenced in past student attempts.");
        }

        List<ChallengeQuestion> cqList = challengeQuestionRepository.findAll().stream()
                .filter(cq -> cq.getQuestion().getId().equals(id))
                .toList();

        List<Long> affectedChallengeIds = cqList.stream()
                .map(cq -> cq.getChallenge().getId())
                .distinct()
                .toList();

        challengeQuestionRepository.deleteAll(cqList);
        questionRepository.delete(question);

        for (Long cid : affectedChallengeIds) {
            recalculateChallengeTotals(cid);
        }

        log.info("[ADMIN] Safely deleted question ID: {}", id);
    }

    private void validateChallengeRequest(AdminChallengeRequest request) {
        if (request == null || request.title() == null || request.title().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge title is required");
        }
        if (request.durationMinutes() == null || request.durationMinutes() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be greater than 0 minutes");
        }
        if (request.category() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }
        if (request.difficulty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Difficulty is required");
        }
        if (request.startTime() != null && request.endTime() != null && request.endTime().isBefore(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time cannot be earlier than start time");
        }
    }

    private void validateQuestionRequest(AdminQuestionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question data is required");
        }
        if (request.questionText() == null || request.questionText().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question text cannot be empty");
        }
        if (request.optionA() == null || request.optionA().trim().isEmpty() ||
            request.optionB() == null || request.optionB().trim().isEmpty() ||
            request.optionC() == null || request.optionC().trim().isEmpty() ||
            request.optionD() == null || request.optionD().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All four options (A, B, C, D) must be provided");
        }
        if (request.correctOption() == null || !request.correctOption().trim().matches("(?i)[ABCD]")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correct option must be A, B, C, or D");
        }
        if (request.points() != null && request.points() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Points must be greater than 0");
        }
    }

    private void recalculateChallengeTotals(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) return;

        List<ChallengeQuestion> cqList = challengeQuestionRepository.findByChallengeIdOrderByQuestionOrderAsc(challengeId);
        int count = cqList.size();
        int points = cqList.stream().mapToInt(cq -> cq.getQuestion().getPoints()).sum();

        challenge.setTotalQuestions(count);
        challenge.setTotalPoints(points);
        challengeRepository.save(challenge);
    }

    // =========================================================================
    // STUDENT MANAGEMENT
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AdminStudentResponse> getAllStudents(String query) {
        List<User> students = userRepository.findByRole(Role.STUDENT);

        if (query != null && !query.trim().isEmpty()) {
            String qLower = query.trim().toLowerCase();
            students = students.stream()
                    .filter(s -> (s.getUsername() != null && s.getUsername().toLowerCase().contains(qLower)) ||
                                 (s.getName() != null && s.getName().toLowerCase().contains(qLower)) ||
                                 (s.getEmail() != null && s.getEmail().toLowerCase().contains(qLower)))
                    .toList();
        }

        List<AdminStudentResponse> list = new ArrayList<>();
        for (User student : students) {
            List<Attempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(student.getId());
            long totalAttempts = attempts.size();
            long completed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.COMPLETED).count();

            int totalPoints = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .mapToInt(Attempt::getPointsEarned)
                    .sum();

            double avgScore = 0.0;
            List<Attempt> finished = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .toList();
            if (!finished.isEmpty()) {
                double pctSum = finished.stream()
                        .mapToDouble(a -> a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0)
                        .sum();
                avgScore = Math.round((pctSum / finished.size()) * 100.0) / 100.0;
            }

            list.add(new AdminStudentResponse(
                    student.getId(),
                    student.getUsername(),
                    student.getName(),
                    student.getEmail(),
                    student.getRole(),
                    student.getCreatedAt(),
                    totalAttempts,
                    completed,
                    totalPoints,
                    avgScore
            ));
        }

        return list;
    }

    @Transactional(readOnly = true)
    public AdminStudentDetailsResponse getStudentDetails(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<Attempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(studentId);
        long totalAttempts = attempts.size();
        long completed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.COMPLETED).count();
        long expired = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.EXPIRED).count();
        long inProgress = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS).count();

        int totalPoints = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                .mapToInt(Attempt::getPointsEarned)
                .sum();

        int bestScore = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                .mapToInt(Attempt::getScore)
                .max()
                .orElse(0);

        double avgScore = 0.0;
        List<Attempt> finished = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                .toList();
        if (!finished.isEmpty()) {
            double pctSum = finished.stream()
                    .mapToDouble(a -> a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0)
                    .sum();
            avgScore = Math.round((pctSum / finished.size()) * 100.0) / 100.0;
        }

        List<UserAttemptSummaryResponse> recentAttempts = challengeService.getUserAttempts(student);

        return new AdminStudentDetailsResponse(
                student.getId(),
                student.getUsername(),
                student.getName(),
                student.getEmail(),
                student.getRole(),
                student.getCreatedAt(),
                totalAttempts,
                completed,
                expired,
                inProgress,
                totalPoints,
                avgScore,
                bestScore,
                recentAttempts
        );
    }

    // =========================================================================
    // ATTEMPT MANAGEMENT & SEARCH
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AdminAttemptResponse> getAllAttempts(Long studentId, String username, Long challengeId,
                                                    Category category, Difficulty difficulty, AttemptStatus status) {
        List<Attempt> list = attemptRepository.findAll();

        if (studentId != null) {
            list = list.stream().filter(a -> a.getUser().getId().equals(studentId)).toList();
        }
        if (username != null && !username.trim().isEmpty()) {
            String uLower = username.trim().toLowerCase();
            list = list.stream().filter(a -> (a.getUser().getUsername() != null && a.getUser().getUsername().toLowerCase().contains(uLower)) ||
                                             (a.getUser().getName() != null && a.getUser().getName().toLowerCase().contains(uLower)) ||
                                             (a.getUser().getEmail() != null && a.getUser().getEmail().toLowerCase().contains(uLower)) ||
                                             (a.getChallenge().getTitle() != null && a.getChallenge().getTitle().toLowerCase().contains(uLower)) ||
                                             String.valueOf(a.getId()).equals(uLower)).toList();
        }
        if (challengeId != null) {
            list = list.stream().filter(a -> a.getChallenge().getId().equals(challengeId)).toList();
        }
        if (category != null) {
            list = list.stream().filter(a -> a.getChallenge().getCategory() == category).toList();
        }
        if (difficulty != null) {
            list = list.stream().filter(a -> a.getChallenge().getDifficulty() == difficulty).toList();
        }
        if (status != null) {
            list = list.stream().filter(a -> a.getStatus() == status).toList();
        }

        list = list.stream().sorted(Comparator.comparing(Attempt::getStartedAt).reversed()).toList();

        return list.stream().map(a -> {
            long timeTaken = Duration.between(a.getStartedAt(),
                    a.getSubmittedAt() != null ? a.getSubmittedAt() : LocalDateTime.now()).getSeconds();
            return new AdminAttemptResponse(
                    a.getId(),
                    a.getUser().getId(),
                    a.getUser().getName(),
                    a.getUser().getUsername(),
                    a.getChallenge().getId(),
                    a.getChallenge().getTitle(),
                    a.getChallenge().getCategory(),
                    a.getChallenge().getDifficulty(),
                    a.getStartedAt(),
                    a.getSubmittedAt(),
                    a.getStatus(),
                    a.getScore(),
                    a.getTotalPoints(),
                    a.getCorrectAnswers(),
                    a.getWrongAnswers(),
                    a.getUnansweredAnswers(),
                    a.getPointsEarned(),
                    timeTaken
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public AdminAttemptDetailsResponse getAttemptDetails(Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

        User student = attempt.getUser();
        Challenge challenge = attempt.getChallenge();

        long timeTakenSeconds = Duration.between(attempt.getStartedAt(),
                attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : LocalDateTime.now()).getSeconds();
        double pct = attempt.getTotalPoints() > 0 ? (double) attempt.getScore() / attempt.getTotalPoints() * 100.0 : 0.0;

        List<AnswerReviewResponse> questionDetails = challengeService.getAnswerReview(challenge.getId(), attempt.getId(), student);

        return new AdminAttemptDetailsResponse(
                attempt.getId(),
                student.getId(),
                student.getName(),
                student.getUsername(),
                challenge.getId(),
                challenge.getTitle(),
                challenge.getCategory(),
                challenge.getDifficulty(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getTotalPoints(),
                Math.round(pct * 100.0) / 100.0,
                attempt.getCorrectAnswers(),
                attempt.getWrongAnswers(),
                attempt.getUnansweredAnswers(),
                attempt.getPointsEarned(),
                timeTakenSeconds,
                questionDetails
        );
    }

    // =========================================================================
    // LEADERBOARD
    // =========================================================================

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard() {
        List<User> students = userRepository.findByRole(Role.STUDENT);

        List<LeaderboardEntryResponse> rawList = new ArrayList<>();
        for (User student : students) {
            List<Attempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(student.getId());

            int totalPoints = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .mapToInt(Attempt::getPointsEarned)
                    .sum();

            long completed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.COMPLETED).count();

            double avgScore = 0.0;
            List<Attempt> finished = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .toList();
            if (!finished.isEmpty()) {
                double pctSum = finished.stream()
                        .mapToDouble(a -> a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0)
                        .sum();
                avgScore = Math.round((pctSum / finished.size()) * 100.0) / 100.0;
            }

            rawList.add(new LeaderboardEntryResponse(
                    0,
                    student.getId(),
                    student.getUsername() != null ? student.getUsername() : "N/A",
                    student.getName(),
                    totalPoints,
                    completed,
                    avgScore
            ));
        }

        rawList.sort(Comparator
                .comparing(LeaderboardEntryResponse::totalPoints).reversed()
                .thenComparing(Comparator.comparing(LeaderboardEntryResponse::completedChallenges).reversed())
                .thenComparing(Comparator.comparing(LeaderboardEntryResponse::averageScore).reversed())
                .thenComparing(LeaderboardEntryResponse::username)
        );

        List<LeaderboardEntryResponse> rankedList = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntryResponse entry : rawList) {
            rankedList.add(new LeaderboardEntryResponse(
                    rank++,
                    entry.userId(),
                    entry.username(),
                    entry.name(),
                    entry.totalPoints(),
                    entry.completedChallenges(),
                    entry.averageScore()
            ));
        }

        return rankedList;
    }

    // =========================================================================
    // REPORTS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ChallengeReportResponse> getChallengeReports() {
        List<Challenge> challenges = challengeRepository.findAll();
        List<ChallengeReportResponse> reportList = new ArrayList<>();

        for (Challenge c : challenges) {
            List<Attempt> attempts = attemptRepository.findByChallengeId(c.getId());
            long totalAttempts = attempts.size();
            long completed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.COMPLETED).count();
            long expired = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.EXPIRED).count();

            int highestScore = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .mapToInt(Attempt::getScore)
                    .max()
                    .orElse(0);

            double avgScore = 0.0;
            List<Attempt> finished = attempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .toList();
            if (!finished.isEmpty()) {
                double pctSum = finished.stream()
                        .mapToDouble(a -> a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0)
                        .sum();
                avgScore = Math.round((pctSum / finished.size()) * 100.0) / 100.0;
            }

            double completionRate = totalAttempts > 0 ? Math.round(((double) completed / totalAttempts * 100.0) * 100.0) / 100.0 : 0.0;

            reportList.add(new ChallengeReportResponse(
                    c.getId(),
                    c.getTitle(),
                    c.getCategory(),
                    c.getDifficulty(),
                    totalAttempts,
                    completed,
                    expired,
                    avgScore,
                    highestScore,
                    completionRate
            ));
        }

        return reportList;
    }

    @Transactional(readOnly = true)
    public List<CategoryReportResponse> getCategoryReports() {
        List<Challenge> challenges = challengeRepository.findAll();
        List<CategoryReportResponse> reportList = new ArrayList<>();

        for (Category cat : Category.values()) {
            List<Challenge> catChallenges = challenges.stream().filter(c -> c.getCategory() == cat).toList();
            long totalChallenges = catChallenges.size();

            List<Long> challengeIds = catChallenges.stream().map(Challenge::getId).toList();
            List<Attempt> catAttempts = attemptRepository.findAll().stream()
                    .filter(a -> challengeIds.contains(a.getChallenge().getId()))
                    .toList();

            long totalAttempts = catAttempts.size();
            long completed = catAttempts.stream().filter(a -> a.getStatus() == AttemptStatus.COMPLETED).count();

            double avgScore = 0.0;
            List<Attempt> finished = catAttempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .toList();
            if (!finished.isEmpty()) {
                double pctSum = finished.stream()
                        .mapToDouble(a -> a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0)
                        .sum();
                avgScore = Math.round((pctSum / finished.size()) * 100.0) / 100.0;
            }

            double completionRate = totalAttempts > 0 ? Math.round(((double) completed / totalAttempts * 100.0) * 100.0) / 100.0 : 0.0;

            reportList.add(new CategoryReportResponse(
                    cat,
                    totalChallenges,
                    totalAttempts,
                    completed,
                    avgScore,
                    completionRate
            ));
        }

        return reportList;
    }

    @Transactional(readOnly = true)
    public List<DifficultyReportResponse> getDifficultyReports() {
        List<Challenge> challenges = challengeRepository.findAll();
        List<DifficultyReportResponse> reportList = new ArrayList<>();

        for (Difficulty diff : Difficulty.values()) {
            List<Challenge> diffChallenges = challenges.stream().filter(c -> c.getDifficulty() == diff).toList();
            long totalChallenges = diffChallenges.size();

            List<Long> challengeIds = diffChallenges.stream().map(Challenge::getId).toList();
            List<Attempt> diffAttempts = attemptRepository.findAll().stream()
                    .filter(a -> challengeIds.contains(a.getChallenge().getId()))
                    .toList();

            long totalAttempts = diffAttempts.size();
            long completed = diffAttempts.stream().filter(a -> a.getStatus() == AttemptStatus.COMPLETED).count();

            double avgScore = 0.0;
            List<Attempt> finished = diffAttempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.COMPLETED || a.getStatus() == AttemptStatus.EXPIRED)
                    .toList();
            if (!finished.isEmpty()) {
                double pctSum = finished.stream()
                        .mapToDouble(a -> a.getTotalPoints() > 0 ? (double) a.getScore() / a.getTotalPoints() * 100.0 : 0.0)
                        .sum();
                avgScore = Math.round((pctSum / finished.size()) * 100.0) / 100.0;
            }

            double completionRate = totalAttempts > 0 ? Math.round(((double) completed / totalAttempts * 100.0) * 100.0) / 100.0 : 0.0;

            reportList.add(new DifficultyReportResponse(
                    diff,
                    totalChallenges,
                    totalAttempts,
                    completed,
                    avgScore,
                    completionRate
            ));
        }

        return reportList;
    }
}
