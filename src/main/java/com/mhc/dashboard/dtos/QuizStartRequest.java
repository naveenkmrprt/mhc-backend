package com.mhc.dashboard.dtos;

import lombok.Data;

@Data
public class QuizStartRequest {
    private String mockMode; // BALANCED_DIAGNOSTIC, OFFICIAL_FORMAT, HISTORICAL_PRACTICE
    private String questionPool;
}
