package com.sms.repository;

import com.sms.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendanceRepository
        extends JpaRepository<AttendanceRecord, Long> {

 List<AttendanceRecord> findByCourseIdOrderByAttendanceDateDesc(Long courseId);

 List<AttendanceRecord> findByStudentIdOrderByAttendanceDateDesc(Long studentId);

 long countByCourseId(Long courseId);
 long countByStudentId(Long studentId);
}