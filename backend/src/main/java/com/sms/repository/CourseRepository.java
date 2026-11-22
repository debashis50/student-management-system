package com.sms.repository;

import com.sms.model.Course;
import com.sms.model.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStatusOrderByTitleAsc(CourseStatus status);
    long countByStatus(CourseStatus status);
    long countByTeacherId(Long teacherId);
}
