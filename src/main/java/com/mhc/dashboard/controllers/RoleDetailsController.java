package com.mhc.dashboard.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/role")
public class RoleDetailsController {

    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getRoleDetails() {
        return ResponseEntity.ok(Map.of(
            "title", "Madras High Court Assistant Programmer",
            "salary", Map.of(
                "range", "Rs. 35,900 – Rs. 1,31,500",
                "payLevel", "Level-13",
                "category", "State Government Job"
            ),
            "allowances", List.of(
                "Dearness Allowance (DA)",
                "House Rent Allowance (HRA)",
                "Travel Allowance (TA)",
                "Other Special Allowances as applicable"
            ),
            "benefits", List.of(
                "Pension under the Tamil Nadu State Government pension scheme",
                "Paid leaves (Earned Leave, Casual Leave, Maternity/Paternity Leave)",
                "Job security and career progression opportunities"
            ),
            "vacancies", "41 (As per 2025 Notification)",
            "bcVacancies", "10 Vacancies for BC (Non-Muslim) [5 Gen, 2 Women, 2 PSTM, 1 Women-PSTM] | 1 Vacancy for BCM",
            "qualifications", List.of(
                "B.Sc. / BCA with 3 years experience in software development",
                "B.E. / B.Tech / MCA / M.Sc with 2 years experience",
                "M.E. / M.Tech with 1 year experience",
                "Specialization: CS, IT, Software Engineering, AI & Machine Learning, or Computer Application"
            ),
            "responsibilities", List.of(
                "Regulated by the Madras High Court Technical Manpower (Appointment & Conditions of Service) Rules, 2017.",
                "Assisting in the maintenance and enhancement of court software systems, including application development.",
                "Supporting data management tasks and routine technical support."
            ),
            "placeOfPosting", "Principal Seat at Chennai, Madurai Bench, or any other District in Tamil Nadu (Liable to be transferred or deputed at any time).",
            "careerGrowth", "Senior Assistant Programmer ➝ Senior Technical Officer/System Analyst ➝ Chief System Analyst"
        ));
    }
}
