package com.mhc.dashboard.dtos;

import lombok.Data;

@Data
public class ErrorClassificationRequest {
    private Long answerId;
    private String errorType; // KNOWLEDGE_GAP, MISREAD_QUESTION, CONCEPT_CONFUSION, LOGIC_ERROR, SYNTAX_OR_API_RECALL, CODE_TRACING_ERROR, TIME_PRESSURE, BLIND_GUESS, AMBIGUOUS_QUESTION, CARELESS_ERROR, OTHER
    private String reviewNote;
}
