package com.sms.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseRequest(
        @NotBlank @Size(max = 20) String courseCode,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 80) String category,
        @Size(max = 2500) String description,
        @Size(max = 5000) String syllabus,
        @NotNull @DecimalMin("0.00") BigDecimal fee,
        @NotNull @Min(1) @Max(520) Integer durationWeeks,
        @NotNull @Min(0) @Max(100) Integer creditHours,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate enrollmentDeadline,
        @NotNull @Min(0) @Max(100000) Integer capacity,
        Long teacherId
) {}
