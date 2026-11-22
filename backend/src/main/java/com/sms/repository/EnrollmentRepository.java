package com.sms.repository;

import com.sms.model.Enrollment;
import com.sms.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(Long studentId);
    List<Enrollment> findByStudentIdAndStatusOrderByEnrolledAtDesc(Long studentId, EnrollmentStatus status);
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    long countByStatus(EnrollmentStatus status);
    List<Enrollment> findByStatusOrderByEnrolledAtDesc(EnrollmentStatus status);
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    List<Enrollment> findByCourseTeacherIdAndStatus(Long teacherId, EnrollmentStatus status);
    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);
    long countByCourseId(Long courseId);
    long countByStudentId(Long studentId);
}
