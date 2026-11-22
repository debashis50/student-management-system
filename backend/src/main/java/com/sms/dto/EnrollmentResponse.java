package com.sms.dto;

import com.sms.model.Enrollment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrollmentResponse(Long enrollmentId, Long courseId, String courseCode,
        String courseTitle, BigDecimal fee, String status, LocalDateTime enrolledAt,
        LocalDateTime droppedAt) {
    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(e.getId(), e.getCourse().getId(), e.getCourse().getCourseCode(),
                e.getCourse().getTitle(), e.getCourse().getFee(), e.getStatus().name(),
                e.getEnrolledAt(), e.getDroppedAt());
    }
}
