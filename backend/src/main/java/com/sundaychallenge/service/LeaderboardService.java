package com.sundaychallenge.service;

import com.sundaychallenge.dto.LeaderboardEntryResponse;
import com.sundaychallenge.dto.LeaderboardPageResponse;
import com.sundaychallenge.dto.LeaderboardUserPositionResponse;
import com.sundaychallenge.entity.Attempt;
import com.sundaychallenge.entity.Role;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.entity.enums.AttemptStatus;
import com.sundaychallenge.entity.enums.Category;
import com.sundaychallenge.entity.enums.Difficulty;
import com.sundaychallenge.repository.AttemptRepository;
import com.sundaychallenge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for calculating authoritative student rankings, top 3 podium entries,
 * platform statistics, current user positions, and filtered leaderboard pages.
 */
@Service
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;

    public LeaderboardService(UserRepository userRepository, AttemptRepository attemptRepository) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
    }

    private static class StudentStats {
        User user;
        Set<Long> attemptedChallengeIds = new HashSet<>();
        Set<Long> completedChallengeIds = new HashSet<>();
        Map<Long, Integer> bestScorePerChallenge = new HashMap<>();
        List<Attempt> completedAttempts = new ArrayList<>();

        StudentStats(User user) {
            this.user = user;
        }

        int getTotalPoints() {
            return bestScorePerChallenge.values().stream().mapToInt(Integer::intValue).sum();
        }

        int getBestScore() {
            return completedAttempts.stream()
                    .mapToInt(a -> Math.max(a.getScore() != null ? a.getScore() : 0, a.getPointsEarned() != null ? a.getPointsEarned() : 0))
                    .max().orElse(0);
        }

        double getAverageScore() {
            if (completedAttempts.isEmpty()) return 0.0;
            double avg = completedAttempts.stream()
                    .mapToDouble(a -> {
                        int tot = (a.getTotalPoints() != null && a.getTotalPoints() > 0) ? a.getTotalPoints() : 100;
                        int sc = Math.max(a.getScore() != null ? a.getScore() : 0, a.getPointsEarned() != null ? a.getPointsEarned() : 0);
                        return ((double) sc / tot) * 100.0;
                    })
                    .average().orElse(0.0);
            return Math.round(avg * 10.0) / 10.0;
        }
    }

    @Transactional(readOnly = true)
    public LeaderboardPageResponse getLeaderboardPage(
            Long challengeId,
            Category category,
            Difficulty difficulty,
            String period,
            String search,
            String participation,
            int page,
            int size,
            User currentUser) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 100));

        log.info("[LEADERBOARD] Fetching leaderboard page. challengeId: {}, category: {}, difficulty: {}, period: {}, search: '{}', participation: '{}', page: {}, size: {}",
                challengeId, category, difficulty, period, search, participation, validPage, validSize);

        // 1. Fetch all user accounts (including students and any participant accounts)
        List<User> students = userRepository.findAll();

        log.info("[LEADERBOARD] Total student accounts found in DB: {}", students.size());

        if (students.isEmpty()) {
            LeaderboardUserPositionResponse userPos = (currentUser != null)
                    ? new LeaderboardUserPositionResponse(0, 0, 0.0, 0, 0, 0, currentUser.getUsername(), currentUser.getName(), currentUser.getProfileImage())
                    : null;
            return new LeaderboardPageResponse(Collections.emptyList(), Collections.emptyList(), userPos, 0, 0, 0, 0.0, validPage, validSize, 0);
        }

        // 2. Determine time period threshold
        LocalDateTime timeThreshold = null;
        if ("THIS_WEEK".equalsIgnoreCase(period)) {
            timeThreshold = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).withHour(0).withMinute(0).withSecond(0).withNano(0);
        } else if ("THIS_MONTH".equalsIgnoreCase(period)) {
            timeThreshold = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0).withNano(0);
        }

        // 3. Process attempts per student
        Map<Long, StudentStats> statsMap = new HashMap<>();

        for (User student : students) {
            StudentStats stats = new StudentStats(student);
            List<Attempt> userAttempts = attemptRepository.findByUserIdOrderByStartedAtDesc(student.getId());

            for (Attempt attempt : userAttempts) {
                if (attempt.getChallenge() == null) continue;

                Long cId = attempt.getChallenge().getId();

                // Apply Challenge, Category, Difficulty, Time filters
                if (challengeId != null && !Objects.equals(cId, challengeId)) continue;
                if (category != null && attempt.getChallenge().getCategory() != category) continue;
                if (difficulty != null && attempt.getChallenge().getDifficulty() != difficulty) continue;
                if (timeThreshold != null && attempt.getStartedAt() != null && attempt.getStartedAt().isBefore(timeThreshold)) continue;

                stats.attemptedChallengeIds.add(cId);

                // Handle valid final attempt statuses: COMPLETED or EXPIRED
                if (attempt.getStatus() == AttemptStatus.COMPLETED || attempt.getStatus() == AttemptStatus.EXPIRED) {
                    stats.completedChallengeIds.add(cId);
                    stats.completedAttempts.add(attempt);
                    int scoreVal = Math.max(attempt.getScore() != null ? attempt.getScore() : 0, attempt.getPointsEarned() != null ? attempt.getPointsEarned() : 0);
                    int currentBest = stats.bestScorePerChallenge.getOrDefault(cId, 0);
                    if (scoreVal >= currentBest) {
                        stats.bestScorePerChallenge.put(cId, scoreVal);
                    }
                }
            }
            statsMap.put(student.getId(), stats);
        }

        // 4. Filter students by search & participation filter
        String cleanSearch = search != null ? search.trim().toLowerCase() : "";

        List<StudentStats> filteredList = statsMap.values().stream()
                .filter(s -> {
                    if (!cleanSearch.isEmpty()) {
                        String roll = s.user.getUsername() != null ? s.user.getUsername().toLowerCase() : "";
                        String name = s.user.getName() != null ? s.user.getName().toLowerCase() : "";
                        String email = s.user.getEmail() != null ? s.user.getEmail().toLowerCase() : "";
                        if (!roll.contains(cleanSearch) && !name.contains(cleanSearch) && !email.contains(cleanSearch)) {
                            return false;
                        }
                    }
                    if ("PARTICIPATED".equalsIgnoreCase(participation) && s.attemptedChallengeIds.isEmpty()) {
                        return false;
                    }
                    if ("NEVER_PARTICIPATED".equalsIgnoreCase(participation) && !s.attemptedChallengeIds.isEmpty()) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 5. Authoritative Ranking Sort Order:
        // 1. Total Points DESC
        // 2. Completed Challenges DESC
        // 3. Average Score DESC
        // 4. Best Score DESC
        // 5. User ID ASC (Deterministic tie-breaker)
        Comparator<StudentStats> rankingComparator = Comparator
                .comparingInt(StudentStats::getTotalPoints)
                .thenComparingLong((StudentStats s) -> (long) s.completedChallengeIds.size())
                .thenComparingDouble(StudentStats::getAverageScore)
                .thenComparingInt(StudentStats::getBestScore)
                .reversed()
                .thenComparingLong(s -> s.user.getId());

        filteredList.sort(rankingComparator);

        log.info("[LEADERBOARD] Aggregated {} students into leaderboard rankings.", filteredList.size());

        // Build ranked entries
        List<LeaderboardEntryResponse> rankedEntries = new ArrayList<>();
        LeaderboardUserPositionResponse currentUserPosition = null;

        long participatingCount = 0;
        int maxPointsOnPlatform = 0;
        double sumAccuracy = 0.0;

        for (int i = 0; i < filteredList.size(); i++) {
            StudentStats s = filteredList.get(i);
            int rank = i + 1;

            int totalPts = s.getTotalPoints();
            long completedCount = s.completedChallengeIds.size();
            long attemptedCount = s.attemptedChallengeIds.size();
            double avgScore = s.getAverageScore();
            int bestScore = s.getBestScore();

            if (attemptedCount > 0) participatingCount++;
            if (totalPts > maxPointsOnPlatform) maxPointsOnPlatform = totalPts;
            sumAccuracy += avgScore;

            String statusStr = completedCount > 0 ? "Active" : (attemptedCount > 0 ? "Participated" : "Never Participated");

            String displayRoll = s.user.getUsername() != null && !s.user.getUsername().isBlank() ? s.user.getUsername() : "N/A";
            String displayName = s.user.getName() != null && !s.user.getName().isBlank() ? s.user.getName() : "Student";

            LeaderboardEntryResponse entry = new LeaderboardEntryResponse(
                    rank,
                    s.user.getId(),
                    displayRoll,
                    displayName,
                    s.user.getProfileImage(),
                    attemptedCount,
                    completedCount,
                    avgScore,
                    bestScore,
                    totalPts,
                    statusStr
            );

            rankedEntries.add(entry);

            log.debug("[LEADERBOARD AGGREGATION] Rank #{}: ID={}, Roll={}, Name={}, TotalPts={}, Completed={}, AvgScore={}, BestScore={}",
                    rank, s.user.getId(), displayRoll, displayName, totalPts, completedCount, avgScore, bestScore);

            if (currentUser != null && Objects.equals(s.user.getId(), currentUser.getId())) {
                currentUserPosition = new LeaderboardUserPositionResponse(
                        rank,
                        totalPts,
                        avgScore,
                        bestScore,
                        completedCount,
                        attemptedCount,
                        displayRoll,
                        displayName,
                        s.user.getProfileImage()
                );
            }
        }

        // Top 3 Podium
        List<LeaderboardEntryResponse> topThree = rankedEntries.stream().limit(3).toList();

        // Platform Summary Statistics
        long totalStudents = students.size();
        double platformAvgScore = participatingCount > 0 ? Math.round((sumAccuracy / participatingCount) * 10.0) / 10.0 : 0.0;

        // Pagination
        int totalElements = rankedEntries.size();
        int totalPages = (int) Math.ceil((double) totalElements / validSize);
        int fromIndex = Math.min(validPage * validSize, totalElements);
        int toIndex = Math.min(fromIndex + validSize, totalElements);

        List<LeaderboardEntryResponse> pageEntries = (fromIndex < totalElements) ? rankedEntries.subList(fromIndex, toIndex) : Collections.emptyList();

        return new LeaderboardPageResponse(
                pageEntries,
                topThree,
                currentUserPosition,
                totalStudents,
                participatingCount,
                maxPointsOnPlatform,
                platformAvgScore,
                validPage,
                validSize,
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public LeaderboardUserPositionResponse getCurrentUserPosition(User currentUser) {
        if (currentUser == null) return null;
        LeaderboardPageResponse page = getLeaderboardPage(null, null, null, "ALL_TIME", null, null, 0, Integer.MAX_VALUE, currentUser);
        return page.currentUserPosition();
    }
}
