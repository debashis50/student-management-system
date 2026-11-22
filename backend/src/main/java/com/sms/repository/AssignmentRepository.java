package com.sms.repository;

import com.sms.model.CourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository
        extends JpaRepository<CourseAssignment, Long> {

 List<CourseAssignment> findAllByOrderByDueDateAsc();

 long countByCourseId(Long courseId);
 long countByCreatedById(Long createdById);
}