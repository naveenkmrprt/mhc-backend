package com.mhc.dashboard.controllers;

import com.mhc.dashboard.models.User;
import com.mhc.dashboard.repositories.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
public class DebugController {

    private final UserRepository userRepository;

    @Value("${app.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${app.bootstrap.password:}")
    private String bootstrapPassword;

    public DebugController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("backend", "up");
        status.put("bootstrapUsername", bootstrapUsername);
        status.put("bootstrapPasswordLength", bootstrapPassword != null ? bootstrapPassword.length() : 0);
        
        try {
            List<User> users = userRepository.findAll();
            status.put("userCount", users.size());
            if (!users.isEmpty()) {
                status.put("firstUser", users.get(0).getUsername());
                status.put("firstUserRole", users.get(0).getRole());
            }
        } catch (Exception e) {
            status.put("dbError", e.getMessage());
        }
        
        return status;
    }
}
