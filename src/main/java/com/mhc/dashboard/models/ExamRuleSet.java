package com.mhc.dashboard.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_rule_sets")
@Data
@NoArgsConstructor
public class ExamRuleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String examCycle; // e.g., 'MHC_AP_2025'

    @Column(length = 100)
    private String notificationNumber;

    private LocalDate notificationDate;

    private String sourceDocument;

    @Column(length = 500)
    private String sourceUrl;

    @Column(length = 50)
    private String sourcePage;

    @Column(columnDefinition = "TEXT")
    private String sourceQuote;

    @Column(length = 50)
    private String verificationStatus; // OFFICIAL_CONFIRMED, OFFICIAL_NOT_FOUND, SECONDARY_SOURCE, INFERRED, USER_ENTERED

    private Integer writtenTotalMarks;
    private Integer partAMarks;
    private Integer partBMarks;
    private Double negativeMarkPerWrongAnswer;
    private Integer skillTestMarks;
    private Integer vivaMarks;

    private Boolean partAFinalMeritIncluded;
    private Boolean partBFinalMeritIncluded;

    @Column(length = 50)
    private String shortlistingRatio; // NULL if unconfirmed

    @Column(length = 50)
    private String shortlistingRatioStatus; // OFFICIAL_NOT_FOUND

    private Boolean active;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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
