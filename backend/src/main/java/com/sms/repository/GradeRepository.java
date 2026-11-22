package com.sms.repository;

import com.sms.model.GradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GradeRepository
        extends JpaRepository<GradeRecord, Long> {

 List<GradeRecord> findByCourseId(Long courseId);

 List<GradeRecord> findByStudentId(Long studentId);

 long countByCourseId(Long courseId);
 long countByStudentId(Long studentId);
}