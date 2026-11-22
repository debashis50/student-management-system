package com.sms.service;

import com.sms.dto.EnrollmentResponse;
import com.sms.dto.EnrollmentSummary;
import com.sms.model.*;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final CurrentUserService currentUser;

    public EnrollmentService(EnrollmentRepository enrollments, CourseRepository courses, CurrentUserService currentUser) {
        this.enrollments = enrollments; this.courses = courses; this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> myEnrollments() {
        AppUser student = currentUser.requireCurrentUser();
        return enrollments.findByStudentIdOrderByEnrolledAtDesc(student.getId()).stream()
                .map(EnrollmentResponse::from).toList();
    }

    @Transactional
    public EnrollmentResponse enroll(Long courseId) {
        AppUser student = currentUser.requireCurrentUser();
        Course course = courses.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (course.getStatus() != CourseStatus.PUBLISHED) throw new IllegalArgumentException("This course is not published");
        if (!course.isEnrollmentOpen()) throw new IllegalArgumentException("Enrollment is closed");
        if (course.getEnrollmentDeadline() != null && LocalDate.now().isAfter(course.getEnrollmentDeadline())) {
            throw new IllegalArgumentException("Enrollment deadline has passed");
        }
        if (course.getCapacity() > 0 && enrollments.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE) >= course.getCapacity()) {
            throw new IllegalArgumentException("Course is full");
        }
        var existing = enrollments.findByStudentIdAndCourseId(student.getId(), courseId);
        if (existing.isPresent() && existing.get().getStatus() == EnrollmentStatus.ACTIVE) {
            throw new IllegalArgumentException("You are already enrolled in this course");
        }
        Enrollment e = existing.orElseGet(Enrollment::new);
        e.setStudent(student);
        e.setCourse(course);
        e.setStatus(EnrollmentStatus.ACTIVE);
        e.setEnrolledAt(LocalDateTime.now());
        e.setDroppedAt(null);
        return EnrollmentResponse.from(enrollments.save(e));
    }

    @Transactional
    public void drop(Long courseId) {
        AppUser student = currentUser.requireCurrentUser();
        Enrollment e = enrollments.findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
        if (e.getStatus() != EnrollmentStatus.ACTIVE) throw new IllegalArgumentException("This course is not currently active");
        Course c = e.getCourse();
        if (c.getEnrollmentDeadline() != null && LocalDate.now().isAfter(c.getEnrollmentDeadline())) {
            throw new IllegalArgumentException("The course drop deadline has passed");
        }
        e.setStatus(EnrollmentStatus.DROPPED);
        e.setDroppedAt(LocalDateTime.now());
        enrollments.save(e);
    }

    @Transactional(readOnly = true)
    public EnrollmentSummary summary() {
        AppUser student = currentUser.requireCurrentUser();
        List<Enrollment> active = enrollments.findByStudentIdAndStatusOrderByEnrolledAtDesc(student.getId(), EnrollmentStatus.ACTIVE);
        BigDecimal total = active.stream().map(e -> e.getCourse().getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EnrollmentSummary(active.size(), total);
    }
}
