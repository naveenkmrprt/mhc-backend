package com.mhc.dashboard.dtos;

import lombok.Data;
import java.util.List;

@Data
public class QuizSubmitRequest {
    private Long version;
    private List<QuizAnswerRequest> answers;
}
