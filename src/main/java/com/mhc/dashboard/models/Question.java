package com.mhc.dashboard.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single MCQ question, linked to a SyllabusTopic.
 * Imported via IngestionController using regex-based parsing.
 */
@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private SyllabusCategory syllabusCategory;

    @Column(name = "micro_topic", length = 255)
    private String microTopic;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "option_a", columnDefinition = "TEXT")
    private String optionA;

    @Column(name = "option_b", columnDefinition = "TEXT")
    private String optionB;

    @Column(name = "option_c", columnDefinition = "TEXT")
    private String optionC;

    @Column(name = "option_d", columnDefinition = "TEXT")
    private String optionD;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;

    @Column(length = 50)
    private String difficultyEstimate = "MEDIUM"; // EASY, MEDIUM, HARD

    @Column(length = 50)
    private String difficultyConfidence; // HIGH, MEDIUM, LOW

    private Double ocrConfidence;

    @Column(length = 255)
    private String duplicateHash;

    // Provenance Fields
    @Column(length = 255)
    private String sourceDocument;

    @Column(length = 500)
    private String sourceUrl;

    @Column(length = 50)
    private String sourcePage;

    @Column(length = 50)
    private String sourceQuestionNumber;

    @Column(columnDefinition = "TEXT")
    private String sourceQuote;

    @Column(length = 50)
    private String verificationStatus; // OFFICIAL_CONFIRMED, SECONDARY_SOURCE, AI_GENERATED, USER_CREATED, QUARANTINED, REJECTED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
