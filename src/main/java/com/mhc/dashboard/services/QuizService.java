package com.mhc.dashboard.services;

import com.mhc.dashboard.dtos.*;
import com.mhc.dashboard.models.*;
import com.mhc.dashboard.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles quiz session lifecycle: creation, answer submission, and scoring.
 * Enforces strict rules from ExamRuleSet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuestionRepository questionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ExamRuleSetRepository examRuleSetRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));
    }

    private ExamRuleSet getActiveRuleSet() {
        return examRuleSetRepository.findByExamCycleAndActiveTrue("MHC_AP_2025")
                .orElseThrow(() -> new IllegalStateException("FAIL CLOSED: Active ExamRuleSet not found for MHC_AP_2025. Cannot proceed without official rules."));
    }

    @Transactional
    public QuizSession startSession(QuizStartRequest request) {
        User currentUser = getCurrentUser();
        ExamRuleSet activeRules = getActiveRuleSet();

        QuizSession session = new QuizSession();
        session.setOwner(currentUser);
        session.setMockMode(request.getMockMode());
        session.setQuestionPool(request.getQuestionPool());
        session.setRuleSetId(activeRules.getId());
        session.setStartedAt(LocalDateTime.now());
        session.setAnalysisStatus("ANALYSIS_PENDING");

        int totalQuestions = 0;
        
        if ("BALANCED_DIAGNOSTIC".equals(request.getMockMode())) {
            // Fetch exactly 10 from each of the 7 Part B categories
            totalQuestions = 70;
            session.setDistributionMethod("10_PER_CATEGORY_DIAGNOSTIC");
        } else if ("OFFICIAL_FORMAT".equals(request.getMockMode())) {
            totalQuestions = 70;
            session.setDistributionMethod("OFFICIAL_DISTRIBUTION");
        } else {
            totalQuestions = 20; // Default practice
            session.setDistributionMethod("RANDOM");
        }
        
        session.setTotalQuestions(totalQuestions);
        quizSessionRepository.save(session);
        
        // Pre-allocate QuizAnswers to enforce exactly these questions are submitted
        List<Question> assignedQuestions = getQuestionsForSession(session);
        for (Question q : assignedQuestions) {
            QuizAnswer answer = new QuizAnswer();
            answer.setSession(session);
            answer.setQuestion(q);
            answer.setErrorType("NOT_ATTEMPTED");
            quizAnswerRepository.save(answer);
        }
        
        return session;
    }

    public List<Question> getAssignedQuestionsForSession(Long sessionId) {
        return quizAnswerRepository.findBySessionId(sessionId).stream()
                .map(QuizAnswer::getQuestion)
                .collect(Collectors.toList());
    }

    public List<Question> getQuestionsForSession(QuizSession session) {
        if ("BALANCED_DIAGNOSTIC".equals(session.getMockMode())) {
            List<String> statuses = List.of("OFFICIAL_CONFIRMED", "SECONDARY_SOURCE");
            List<Question> questions = new ArrayList<>();
            // Category IDs 2 to 8 are Part B in our seed data
            for (long i = 2; i <= 8; i++) {
                List<Question> catQs = questionRepository.findRandomByCategoryAndStatusIn(i, statuses, 10);
                if (catQs.size() < 10) {
                    throw new IllegalStateException("Insufficient questions in category " + i + " for BALANCED_DIAGNOSTIC.");
                }
                questions.addAll(catQs);
            }
            return questions;
        } else {
            List<String> statuses = List.of("OFFICIAL_CONFIRMED", "SECONDARY_SOURCE");
            return questionRepository.findRandomByPartAndStatusIn("B", statuses, session.getTotalQuestions());
        }
    }

    @Transactional
    public QuizSession submitSession(Long sessionId, QuizSubmitRequest request, boolean isFinalSubmit) {
        User currentUser = getCurrentUser();
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        if (!session.getOwner().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this session.");
        }

        ExamRuleSet activeRules = examRuleSetRepository.findById(session.getRuleSetId())
                .orElseThrow(() -> new IllegalStateException("RuleSet missing for session."));

        if (!"IN_PROGRESS".equals(session.getSessionStatus())) {
            throw new IllegalStateException("Session is already submitted. Cannot modify answers.");
        }

        // Handle optimistic locking by setting version from request (if we had it, but JPA checks it on save if mapped)
        if (request.getVersion() != null) {
            session.setVersion(request.getVersion());
        }

        // Fetch pre-allocated answers
        List<QuizAnswer> existingAnswers = quizAnswerRepository.findBySessionId(sessionId);
        Map<Long, QuizAnswer> answerMap = existingAnswers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));
        
        int correct = 0, wrong = 0, unattempted = session.getTotalQuestions();
        
        for (QuizAnswerRequest ansReq : request.getAnswers()) {
            QuizAnswer answer = answerMap.get(ansReq.getQuestionId());
            if (answer == null) {
                // Client sent a question that was not assigned to this session
                continue; 
            }
            
            Question q = answer.getQuestion();
            answer.setSelectedOption(ansReq.getSelectedOption());
            answer.setIsGuess(ansReq.getIsGuess());
            answer.setIsSkipped(ansReq.getIsSkipped());
            answer.setConfidenceLevel(ansReq.getConfidenceLevel());
            answer.setTimeSpentSeconds(ansReq.getTimeSpentSeconds());
            
            if (ansReq.getIsSkipped() != null && ansReq.getIsSkipped()) {
                answer.setIsCorrect(false);
                answer.setErrorType("NOT_ATTEMPTED");
                // still counts as unattempted
            } else if (ansReq.getSelectedOption() == null || ansReq.getSelectedOption().isEmpty()) {
                answer.setIsCorrect(false);
                answer.setErrorType("NOT_ATTEMPTED");
                // unattempted
            } else if (q.getCorrectOption().equalsIgnoreCase(ansReq.getSelectedOption())) {
                answer.setIsCorrect(true);
                correct++;
                unattempted--;
            } else {
                answer.setIsCorrect(false);
                wrong++;
                unattempted--;
            }
            
            quizAnswerRepository.save(answer);
        }

        if (isFinalSubmit) {
            session.setCorrectAnswers(correct);
            session.setWrongAnswers(wrong);
            session.setUnattempted(unattempted);
            
            // Strict server-side calculation using ExamRuleSet
            double rawScore = correct - (wrong * activeRules.getNegativeMarkPerWrongAnswer());
            session.setRawScore(rawScore);
            
            int attempted = correct + wrong;
            session.setAccuracyPct(attempted > 0 ? ((double) correct / attempted) * 100.0 : 0.0);
            session.setCompletedAt(LocalDateTime.now());
            session.setSessionStatus("SUBMITTED");
            session.setAnalysisStatus("ANALYSIS_PENDING");
        }
        
        return quizSessionRepository.save(session);
    }

    @Transactional
    public QuizSession classifyErrors(Long sessionId, ErrorClassificationPayload payload) {
        User currentUser = getCurrentUser();
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getOwner().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this session.");
        }

        for (ErrorClassificationRequest classReq : payload.getClassifications()) {
            QuizAnswer answer = quizAnswerRepository.findBySessionIdAndQuestionId(sessionId, classReq.getAnswerId())
                    .orElseThrow(() -> new RuntimeException("Answer not found"));
            answer.setErrorType(classReq.getErrorType());
            answer.setReviewNote(classReq.getReviewNote());
            answer.setClassificationCompleted(true);
            quizAnswerRepository.save(answer);
        }
        
        // Check if all needed classifications are done
        List<QuizAnswer> allAnswers = quizAnswerRepository.findBySessionId(sessionId);
        boolean allDone = allAnswers.stream().allMatch(a -> 
            (a.getIsCorrect() != null && a.getIsCorrect() && (a.getIsGuess() == null || !a.getIsGuess())) ||
            (a.getClassificationCompleted() != null && a.getClassificationCompleted()) ||
            "NOT_ATTEMPTED".equals(a.getErrorType())
        );

        if (allDone) {
            session.setAnalysisStatus("COMPLETED");
            quizSessionRepository.save(session);
        }
        
        return session;
    }
}
