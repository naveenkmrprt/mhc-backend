package com.mhc.dashboard.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Daily aggregated performance log for dashboard chart rendering.
 */
@Entity
@Table(name = "daily_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_date", nullable = false, unique = true)
    private LocalDate logDate;

    @Column(name = "questions_attempted")
    private Integer questionsAttempted = 0;

    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;

    @Column(name = "score")
    private Double score = 0.0;

    @Column(name = "accuracy_pct")
    private Double accuracyPct = 0.0;

    @Column(name = "study_time_minutes")
    private Integer studyTimeMinutes = 0;

    /**
     * JSON array of topic names where accuracy < 50%
     * e.g., ["OSI Model", "DBMS Normalization"]
     */
    @Column(name = "weak_topics_json", columnDefinition = "TEXT")
    private String weakTopicsJson = "[]";
}
