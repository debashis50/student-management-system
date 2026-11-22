package com.sms.repository;

import com.sms.model.CourseNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticeRepository
        extends JpaRepository<CourseNotice, Long> {

 List<CourseNotice> findAllByOrderByCreatedAtDesc();

 long countByCourseId(Long courseId);
 long countByCreatedById(Long createdById);
}