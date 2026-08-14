package com.mhc.dashboard.dtos;

import lombok.Data;

@Data
public class QuizAnswerRequest {
    private Long questionId;
    private String selectedOption;
    private Boolean isGuess;
    private Boolean isSkipped;
    private String confidenceLevel; // HIGH, MEDIUM, LOW
    private Integer timeSpentSeconds;
}
