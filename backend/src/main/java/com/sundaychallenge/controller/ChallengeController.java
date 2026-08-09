package com.sundaychallenge.controller;

import com.sundaychallenge.dto.AnswerReviewResponse;
import com.sundaychallenge.dto.ChallengeDetailsResponse;
import com.sundaychallenge.dto.ChallengeResultResponse;
import com.sundaychallenge.dto.ChallengeSummaryResponse;
import com.sundaychallenge.dto.StartChallengeResponse;
import com.sundaychallenge.dto.SubmitChallengeRequest;
import com.sundaychallenge.dto.SubmitChallengeResponse;
import com.sundaychallenge.entity.User;
import com.sundaychallenge.service.AuthService;
import com.sundaychallenge.service.ChallengeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ChallengeController providing REST APIs for challenge discovery, attempt execution,
 * server-side evaluation, results, and answer review.
 */
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private static final Logger log = LoggerFactory.getLogger(ChallengeController.class);

    private final ChallengeService challengeService;
    private final AuthService authService;

    public ChallengeController(ChallengeService challengeService, AuthService authService) {
        this.challengeService = challengeService;
        this.authService = authService;
    }

    /**
     * GET /api/challenges
     * Returns list of all active challenges.
     */
    @GetMapping
    public ResponseEntity<List<ChallengeSummaryResponse>> getActiveChallenges() {
        log.info("[DEBUG] GET /api/challenges called.");
        return ResponseEntity.ok(challengeService.getActiveChallenges());
    }

    /**
     * GET /api/challenges/{id}
     * Returns detailed metadata for a challenge.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChallengeDetailsResponse> getChallengeDetails(@PathVariable("id") Long id) {
        log.info("[DEBUG] GET /api/challenges/{} called.", id);
        return ResponseEntity.ok(challengeService.getChallengeDetails(id));
    }

    /**
     * POST /api/challenges/{id}/start
     * Starts a new attempt (or reuses active IN_PROGRESS attempt) for the authenticated user and returns questions.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<StartChallengeResponse> startChallenge(@PathVariable("id") Long id,
                                                                 @AuthenticationPrincipal OAuth2User oauth2User) {
        User user = authService.getAuthenticatedUser(oauth2User);
        log.info("[DEBUG] POST /api/challenges/{}/start called by user ID: {}", id, user.getId());
        return ResponseEntity.ok(challengeService.startChallenge(id, user));
    }

    /**
     * GET /api/challenges/{id}/attempt/{attemptId}
     * Retrieves questions for an existing IN_PROGRESS attempt without creating a new attempt.
     */
    @GetMapping("/{id}/attempt/{attemptId}")
    public ResponseEntity<StartChallengeResponse> getAttemptQuestions(@PathVariable("id") Long id,
                                                                       @PathVariable("attemptId") Long attemptId,
                                                                       @AuthenticationPrincipal OAuth2User oauth2User) {
        User user = authService.getAuthenticatedUser(oauth2User);
        log.info("[DEBUG] GET /api/challenges/{}/attempt/{} called by user ID: {}", id, attemptId, user.getId());
        return ResponseEntity.ok(challengeService.getAttemptQuestions(id, attemptId, user));
    }

    /**
     * POST /api/challenges/{id}/submit
     * Submits student answers, performs authoritative evaluation, and returns results.
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmitChallengeResponse> submitChallenge(@PathVariable("id") Long id,
                                                                   @RequestBody SubmitChallengeRequest request,
                                                                   @AuthenticationPrincipal OAuth2User oauth2User) {
        User user = authService.getAuthenticatedUser(oauth2User);
        log.info("[DEBUG] POST /api/challenges/{}/submit called by user ID: {} for attempt ID: {}",
                id, user.getId(), (request != null ? request.attemptId() : null));
        return ResponseEntity.ok(challengeService.submitChallenge(id, request, user));
    }

    /**
     * GET /api/challenges/{id}/result/{attemptId}
     * Returns evaluated result summary for an attempt.
     */
    @GetMapping("/{id}/result/{attemptId}")
    public ResponseEntity<ChallengeResultResponse> getAttemptResult(@PathVariable("id") Long id,
                                                                     @PathVariable("attemptId") Long attemptId,
                                                                     @AuthenticationPrincipal OAuth2User oauth2User) {
        User user = authService.getAuthenticatedUser(oauth2User);
        log.info("[DEBUG] GET /api/challenges/{}/result/{} called by user ID: {}", id, attemptId, user.getId());
        return ResponseEntity.ok(challengeService.getAttemptResult(id, attemptId, user));
    }

    /**
     * GET /api/challenges/{id}/result/{attemptId}/review
     * Returns post-submission answer review including correct options & explanations.
     */
    @GetMapping("/{id}/result/{attemptId}/review")
    public ResponseEntity<List<AnswerReviewResponse>> getAnswerReview(@PathVariable("id") Long id,
                                                                       @PathVariable("attemptId") Long attemptId,
                                                                       @AuthenticationPrincipal OAuth2User oauth2User) {
        User user = authService.getAuthenticatedUser(oauth2User);
        log.info("[DEBUG] GET /api/challenges/{}/result/{}/review called by user ID: {}", id, attemptId, user.getId());
        return ResponseEntity.ok(challengeService.getAnswerReview(id, attemptId, user));
    }
}
