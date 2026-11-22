package com.sms.dto;

import com.sms.model.Course;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseResponse(
        Long id, String courseCode, String title, String category, String description,
        String syllabus, BigDecimal fee, Integer durationWeeks, Integer creditHours,
        LocalDate startDate, LocalDate endDate, LocalDate enrollmentDeadline,
        Integer capacity, String status, boolean enrollmentOpen, Long teacherId,
        String teacherName, long enrolledCount
) {
    public static CourseResponse from(Course c, long enrolledCount) {
        return new CourseResponse(c.getId(), c.getCourseCode(), c.getTitle(), c.getCategory(),
                c.getDescription(), c.getSyllabus(), c.getFee(), c.getDurationWeeks(),
                c.getCreditHours(), c.getStartDate(), c.getEndDate(), c.getEnrollmentDeadline(),
                c.getCapacity(), c.getStatus().name(), c.isEnrollmentOpen(),
                c.getTeacher() == null ? null : c.getTeacher().getId(),
                c.getTeacher() == null ? null : c.getTeacher().getFullName(), enrolledCount);
    }
}
