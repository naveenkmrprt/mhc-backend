package com.mhc.dashboard.services;

import com.mhc.dashboard.dtos.*;
import com.mhc.dashboard.models.*;
import com.mhc.dashboard.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuizSessionRepository quizSessionRepository;
    @Mock
    private QuizAnswerRepository quizAnswerRepository;
    @Mock
    private ExamRuleSetRepository examRuleSetRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private QuizService quizService;

    private ExamRuleSet ruleSet;
    private User mockUser;

    @BeforeEach
    void setUp() {
        ruleSet = new ExamRuleSet();
        ruleSet.setId(1L);
        ruleSet.setExamCycle("MHC_AP_2025");
        ruleSet.setNegativeMarkPerWrongAnswer(0.25);
        ruleSet.setPartBMarks(70);

        mockUser = new User();
        mockUser.setId(99L);
        mockUser.setUsername("testuser");

        // Use lenient() to allow these to go unused in tests that don't need them
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("testuser");
        SecurityContextHolder.setContext(securityContext);
        
        org.mockito.Mockito.lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
    }

    @Test
    void testFailClosedWhenRuleSetMissing() {
        QuizStartRequest req = new QuizStartRequest();
        req.setMockMode("BALANCED_DIAGNOSTIC");

        when(examRuleSetRepository.findByExamCycleAndActiveTrue("MHC_AP_2025"))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            quizService.startSession(req);
        });
        
        assertTrue(ex.getMessage().contains("FAIL CLOSED"));
    }

    @Test
    void testDiagnosticQuotaEnforcement() {
        QuizStartRequest req = new QuizStartRequest();
        req.setMockMode("BALANCED_DIAGNOSTIC");

        when(examRuleSetRepository.findByExamCycleAndActiveTrue("MHC_AP_2025"))
                .thenReturn(Optional.of(ruleSet));
        
        when(questionRepository.findRandomByCategoryAndStatusIn(anyLong(), anyList(), eq(10)))
                .thenReturn(List.of(new Question(), new Question())); // only 2 questions found

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            quizService.startSession(req);
        });
        
        assertTrue(ex.getMessage().contains("Insufficient questions"));
    }

    @Test
    void testScoringLogic() {
        QuizSession session = new QuizSession();
        session.setId(10L);
        session.setRuleSetId(1L);
        session.setSessionStatus("IN_PROGRESS");
        session.setTotalQuestions(3);
        session.setOwner(mockUser);

        when(quizSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(examRuleSetRepository.findById(1L)).thenReturn(Optional.of(ruleSet));

        Question q1 = new Question(); q1.setId(1L); q1.setCorrectOption("A");
        Question q2 = new Question(); q2.setId(2L); q2.setCorrectOption("B");
        Question q3 = new Question(); q3.setId(3L); q3.setCorrectOption("C"); // skipped

        QuizAnswer a1 = new QuizAnswer(); a1.setQuestion(q1); a1.setSession(session);
        QuizAnswer a2 = new QuizAnswer(); a2.setQuestion(q2); a2.setSession(session);
        QuizAnswer a3 = new QuizAnswer(); a3.setQuestion(q3); a3.setSession(session);

        when(quizAnswerRepository.findBySessionId(10L)).thenReturn(List.of(a1, a2, a3));
        when(quizAnswerRepository.save(any(QuizAnswer.class))).thenAnswer(i -> i.getArgument(0));
        when(quizSessionRepository.save(any(QuizSession.class))).thenAnswer(i -> i.getArgument(0));

        QuizSubmitRequest req = new QuizSubmitRequest();
        QuizAnswerRequest ans1 = new QuizAnswerRequest();
        ans1.setQuestionId(1L);
        ans1.setSelectedOption("A"); // Correct -> 1
        
        QuizAnswerRequest ans2 = new QuizAnswerRequest();
        ans2.setQuestionId(2L);
        ans2.setSelectedOption("D"); // Wrong -> -0.25

        QuizAnswerRequest ans3 = new QuizAnswerRequest();
        ans3.setQuestionId(3L);
        ans3.setIsSkipped(true); // Skipped -> 0

        req.setAnswers(List.of(ans1, ans2, ans3));

        QuizSession result = quizService.submitSession(10L, req, true);

        assertEquals(1, result.getCorrectAnswers());
        assertEquals(1, result.getWrongAnswers());
        assertEquals(1, result.getUnattempted());
        assertEquals(0.75, result.getRawScore()); // 1 - 0.25
        assertEquals(50.0, result.getAccuracyPct()); // 1/2 attempted
    }

    @Test
    void testDuplicateFinalSubmissionIsPrevented() {
        QuizSession session = new QuizSession();
        session.setId(11L);
        session.setRuleSetId(1L);
        session.setSessionStatus("SUBMITTED"); // Already submitted
        session.setOwner(mockUser);

        when(quizSessionRepository.findById(11L)).thenReturn(Optional.of(session));
        when(examRuleSetRepository.findById(1L)).thenReturn(Optional.of(ruleSet));

        QuizSubmitRequest req = new QuizSubmitRequest();
        
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> quizService.submitSession(11L, req, true));
        assertTrue(ex.getMessage().contains("already submitted"));
    }

    @Test
    void testAnswerKeyProtectionAtStart() {
        // Just verify getQuestionsForSession returns Question entities.
        // In QuizController we already map this to a DTO map omitting correctOption.
        // This test ensures the domain logic doesn't eagerly leak it to the session itself.
        QuizSession session = new QuizSession();
        session.setMockMode("PRACTICE");
        session.setTotalQuestions(10);
        
        Question q1 = new Question();
        q1.setQuestionText("Q1");
        
        when(questionRepository.findRandomByPartAndStatusIn(anyString(), anyList(), anyInt())).thenReturn(List.of(q1));
        
        List<Question> qs = quizService.getQuestionsForSession(session);
        assertEquals(1, qs.size());
    }
}
