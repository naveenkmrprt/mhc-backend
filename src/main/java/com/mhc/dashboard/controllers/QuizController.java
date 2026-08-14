package com.mhc.dashboard.controllers;

import com.mhc.dashboard.dtos.*;
import com.mhc.dashboard.models.Question;
import com.mhc.dashboard.models.QuizSession;
import com.mhc.dashboard.services.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startQuiz(@RequestBody QuizStartRequest request) {
        QuizSession session = quizService.startSession(request);
        List<Question> questions = quizService.getQuestionsForSession(session);

        List<Map<String, Object>> safeQuestions = questions.stream().map(q -> {
            Map<String, Object> safe = new java.util.LinkedHashMap<>();
            safe.put("id", q.getId());
            safe.put("questionText", q.getQuestionText());
            safe.put("optionA", q.getOptionA());
            safe.put("optionB", q.getOptionB());
            safe.put("optionC", q.getOptionC());
            safe.put("optionD", q.getOptionD());
            safe.put("verificationStatus", q.getVerificationStatus());
            safe.put("topicName", q.getSyllabusCategory() != null ? q.getSyllabusCategory().getName() : "General");
            return safe;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "sessionId", session.getId(),
                "mockMode", session.getMockMode(),
                "totalQuestions", session.getTotalQuestions(),
                "questions", safeQuestions,
                "version", session.getVersion() != null ? session.getVersion() : 0L
        ));
    }

    @PostMapping("/{sessionId}/autosave")
    public ResponseEntity<QuizSession> autosaveQuiz(
            @PathVariable Long sessionId,
            @RequestBody QuizSubmitRequest request) {
        QuizSession result = quizService.submitSession(sessionId, request, false);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<QuizSession> submitQuiz(
            @PathVariable Long sessionId,
            @RequestBody QuizSubmitRequest request) {
        QuizSession result = quizService.submitSession(sessionId, request, true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{sessionId}/classify-errors")
    public ResponseEntity<QuizSession> classifyErrors(
            @PathVariable Long sessionId,
            @RequestBody ErrorClassificationPayload payload) {
        QuizSession result = quizService.classifyErrors(sessionId, payload);
        return ResponseEntity.ok(result);
    }
}
