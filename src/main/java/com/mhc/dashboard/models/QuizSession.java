package com.mhc.dashboard.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Records a completed quiz/mock exam session with full scoring metrics.
 * Score = correct - (wrong * 0.25) for Part D negative marking.
 */
@Entity
@Table(name = "quiz_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_type", length = 20)
    private String sessionType = "PRACTICE"; // "PRACTICE" or "MOCK_EXAM"

    @Column(length = 50)
    private String mockMode; // BALANCED_DIAGNOSTIC, OFFICIAL_FORMAT, HISTORICAL_PRACTICE, CUSTOM_TOPIC, SKILL_TEST_PRACTICE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @Version
    private Long version;

    private Long ruleSetId;

    @Column(length = 50)
    private String questionPool;

    @Column(length = 100)
    private String distributionMethod;

    @Column(length = 255)
    private String distributionSource;

    @Column(length = 50)
    private String distributionConfidence;

    @Column(length = 50)
    private String analysisStatus = "ANALYSIS_PENDING"; // ANALYSIS_PENDING, COMPLETED

    @Column(length = 50)
    private String sessionStatus = "IN_PROGRESS"; // IN_PROGRESS, SUBMITTED

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_questions")
    private Integer totalQuestions = 0;

    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;

    @Column(name = "wrong_answers")
    private Integer wrongAnswers = 0;

    @Column(name = "unattempted")
    private Integer unattempted = 0;

    /**
     * Raw score after negative marking: correct - (wrong * 0.25)
     */
    @Column(name = "raw_score")
    private Double rawScore = 0.0;

    /**
     * Accuracy percentage: (correct / attempted) * 100
     */
    @Column(name = "accuracy_pct")
    private Double accuracyPct = 0.0;

    /**
     * Total time taken in seconds
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds = 0L;

    /**
     * JSON array of weak topic names e.g., ["OSI Model", "DBMS Normalization"]
     */
    @Column(name = "weak_topics_json", columnDefinition = "TEXT")
    private String weakTopicsJson = "[]";

    /**
     * Calculate and set the score based on answers.
     * Applies 0.25 negative marking for wrong answers (Part D behavior).
     */
    public void calculateScore(boolean applyNegativeMarking) {
        int attempted = correctAnswers + wrongAnswers;
        this.rawScore = applyNegativeMarking
                ? correctAnswers - (wrongAnswers * 0.25)
                : (double) correctAnswers;
        this.accuracyPct = attempted > 0
                ? ((double) correctAnswers / attempted) * 100.0
                : 0.0;
        this.unattempted = totalQuestions - attempted;
    }
}
