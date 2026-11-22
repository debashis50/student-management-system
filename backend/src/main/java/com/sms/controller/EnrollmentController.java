package com.sms.controller;

import com.sms.dto.EnrollmentResponse;
import com.sms.dto.EnrollmentSummary;
import com.sms.service.EnrollmentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final EnrollmentService service;
    public EnrollmentController(EnrollmentService service) { this.service = service; }

    @GetMapping("/my")
    public List<EnrollmentResponse> mine() { return service.myEnrollments(); }

    @GetMapping("/my/summary")
    public EnrollmentSummary summary() { return service.summary(); }

    @PostMapping("/{courseId}")
    public EnrollmentResponse enroll(@PathVariable Long courseId) { return service.enroll(courseId); }

    @DeleteMapping("/{courseId}")
    public Map<String, String> drop(@PathVariable Long courseId) {
        service.drop(courseId);
        return Map.of("message", "Course dropped successfully. Total amount is recalculated from active enrollments.");
    }
}
