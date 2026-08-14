package com.mhc.dashboard.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "quiz_answers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private QuizSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    // Phase 3 provenance and taxonomy
    @Column(name = "is_guess")
    private Boolean isGuess;

    @Column(name = "is_skipped")
    private Boolean isSkipped;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel; // HIGH, MEDIUM, LOW

    @Column(name = "error_type", length = 50)
    private String errorType; // KNOWLEDGE_GAP, MISREAD_QUESTION, CONCEPT_CONFUSION, LOGIC_ERROR, SYNTAX_OR_API_RECALL, CODE_TRACING_ERROR, TIME_PRESSURE, BLIND_GUESS, AMBIGUOUS_QUESTION, CARELESS_ERROR, OTHER

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "classification_completed")
    private Boolean classificationCompleted = false;
}
