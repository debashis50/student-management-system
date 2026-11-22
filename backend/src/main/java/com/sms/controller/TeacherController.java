package com.sms.controller;

import com.sms.dto.CourseResponse;
import com.sms.model.EnrollmentStatus;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final CurrentUserService currentUser;

    public TeacherController(CourseRepository courses, EnrollmentRepository enrollments, CurrentUserService currentUser) {
        this.courses = courses; this.enrollments = enrollments; this.currentUser = currentUser;
    }

    @GetMapping("/courses")
    public List<CourseResponse> myCourses() {
        var teacher = currentUser.requireCurrentUser();
        return courses.findAll().stream().filter(c -> c.getTeacher() != null && c.getTeacher().getId().equals(teacher.getId()))
                .map(c -> CourseResponse.from(c, enrollments.countByCourseIdAndStatus(c.getId(), EnrollmentStatus.ACTIVE)))
                .toList();
    }
}
