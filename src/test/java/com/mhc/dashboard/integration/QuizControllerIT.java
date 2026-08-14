package com.mhc.dashboard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mhc.dashboard.dtos.*;
import com.mhc.dashboard.models.QuizSession;
import com.mhc.dashboard.models.User;
import com.mhc.dashboard.repositories.QuizSessionRepository;
import com.mhc.dashboard.repositories.UserRepository;
import com.mhc.dashboard.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class QuizControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String user1Token;
    private String user2Token;
    private User user1;

    @BeforeEach
    void setup() {
        quizSessionRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User();
        user1.setUsername("user1");
        user1.setPasswordHash(passwordEncoder.encode("pass"));
        user1.setRole("ROLE_USER");
        user1 = userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPasswordHash(passwordEncoder.encode("pass"));
        user2.setRole("ROLE_USER");
        user2 = userRepository.save(user2);

        org.springframework.security.core.userdetails.User userDetails1 = 
            new org.springframework.security.core.userdetails.User("user1", "pass", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        org.springframework.security.core.userdetails.User userDetails2 = 
            new org.springframework.security.core.userdetails.User("user2", "pass", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            
        user1Token = "Bearer " + jwtUtil.generateToken(userDetails1);
        user2Token = "Bearer " + jwtUtil.generateToken(userDetails2);
    }

    @Test
    void testStartQuizDoesNotLeakAnswerKey() throws Exception {
        QuizStartRequest req = new QuizStartRequest();
        req.setMockMode("PRACTICE");

        mockMvc.perform(post("/api/v1/quiz/start")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].correctOption").doesNotExist())
                .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    void testCrossUserAccessPrevented() throws Exception {
        // User 1 starts quiz
        QuizStartRequest req = new QuizStartRequest();
        req.setMockMode("PRACTICE");

        MvcResult result = mockMvc.perform(post("/api/v1/quiz/start")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Long sessionId = objectMapper.readTree(responseStr).get("sessionId").asLong();

        // User 2 tries to autosave to User 1's session
        QuizSubmitRequest submitReq = new QuizSubmitRequest();
        submitReq.setVersion(0L);
        submitReq.setAnswers(List.of());

        mockMvc.perform(post("/api/v1/quiz/" + sessionId + "/autosave")
                .header("Authorization", user2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testOptimisticLockingConcurrentAutosave() throws Exception {
        // User 1 starts quiz
        QuizStartRequest req = new QuizStartRequest();
        req.setMockMode("PRACTICE");

        MvcResult result = mockMvc.perform(post("/api/v1/quiz/start")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long sessionId = objectMapper.readTree(result.getResponse().getContentAsString()).get("sessionId").asLong();

        QuizSubmitRequest submitReq1 = new QuizSubmitRequest();
        submitReq1.setVersion(0L);
        submitReq1.setAnswers(List.of());

        QuizSubmitRequest submitReq2 = new QuizSubmitRequest();
        submitReq2.setVersion(0L); // Stale version
        submitReq2.setAnswers(List.of());

        // First autosave succeeds
        mockMvc.perform(post("/api/v1/quiz/" + sessionId + "/autosave")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        // Second autosave with stale version fails with 409
        mockMvc.perform(post("/api/v1/quiz/" + sessionId + "/autosave")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("STALE_SESSION_VERSION"));
    }
    
    @Test
    void testDuplicateFinalSubmission() throws Exception {
        // User 1 starts quiz
        QuizStartRequest req = new QuizStartRequest();
        req.setMockMode("PRACTICE");

        MvcResult result = mockMvc.perform(post("/api/v1/quiz/start")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Long sessionId = objectMapper.readTree(result.getResponse().getContentAsString()).get("sessionId").asLong();

        QuizSubmitRequest submitReq = new QuizSubmitRequest();
        submitReq.setVersion(0L);
        submitReq.setAnswers(List.of());

        // First submit succeeds
        mockMvc.perform(post("/api/v1/quiz/" + sessionId + "/submit")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk());

        // Second submit fails (already submitted)
        submitReq.setVersion(1L);
        mockMvc.perform(post("/api/v1/quiz/" + sessionId + "/submit")
                .header("Authorization", user1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isBadRequest()); // IllegalStateException maps to 400
    }
}
