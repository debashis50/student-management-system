package com.sms.controller;

import com.sms.model.CourseStatus;
import com.sms.model.EnrollmentStatus;
import com.sms.model.Role;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import com.sms.model.Enrollment;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;

    public AdminController(UserRepository users, CourseRepository courses, EnrollmentRepository enrollments) {
        this.users = users; this.courses = courses; this.enrollments = enrollments;
    }

    @GetMapping("/dashboard")
    public Map<String, Long> dashboard() {
        return Map.of(
                "totalStudents", users.countByRole(Role.STUDENT),
                "activeStudents", users.countByRoleAndActive(Role.STUDENT, true),
                "inactiveStudents", users.countByRoleAndActive(Role.STUDENT, false),
                "totalTeachers", users.countByRole(Role.TEACHER),
                "totalCourses", courses.count(),
                "publishedCourses", courses.countByStatus(CourseStatus.PUBLISHED),
                "draftCourses", courses.countByStatus(CourseStatus.DRAFT),
                "archivedCourses", courses.countByStatus(CourseStatus.ARCHIVED),
                "activeEnrollments", enrollments.countByStatus(EnrollmentStatus.ACTIVE),
                "droppedEnrollments", enrollments.countByStatus(EnrollmentStatus.DROPPED)
        );
    }
}
